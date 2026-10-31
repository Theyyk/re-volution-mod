package com.example.revolution.persistence;

import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.rune.rules.RuneStorageRules;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible embedded Mastery persistence in the existing players document. */
public final class MongoPrestigeRepository implements PrestigeRepository {
    private static final int SCHEMA_VERSION = 3;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoPrestigeRepository(MongoConnection connection, boolean enabled) { this.connection = connection; this.enabled = enabled; }

    @Override public PrestigeState load(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return new PrestigeState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new PrestigeState() : decode(player.get("prestige", Document.class));
    }

    @Override public void save(UUID playerUuid, PrestigeState state) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()), new Document("$set", new Document("prestige", encode(state))), new UpdateOptions().upsert(true));
    }

    @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState nextState) {
        return commitReset(playerUuid, expectedRunId, nextState, PrestigeRepository.ResetCommitPolicy.normal());
    }

    @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState nextState,
                                                    PrestigeRepository.ResetCommitPolicy policy) {
        if (!enabled || !connection.isConnected()) return ResetCommitResult.STALE;
        if (policy == null) return ResetCommitResult.POLICY_UNAVAILABLE;
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        if (player == null) return ResetCommitResult.STALE;
        PrestigeState current = decode(player.get("prestige", Document.class));
        if (current.activeRun == null) return expectedRunId != null && expectedRunId.equals(current.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
        if (expectedRunId == null || !expectedRunId.equals(current.activeRun.runId)) return ResetCommitResult.STALE;
        if (crystals(player) < policy.crystalCost) return ResetCommitResult.INSUFFICIENT_CRYSTALS;

        List<Document> resetCollections = policy.preserveRunes ? null : resetRuneRanks(player);
        Document resetPets = policy.preservePets ? null : MongoPetRepository.resetForPrestige(player);
        Document set = new Document("prestige", encode(nextState));
        if (resetCollections != null) set.append("decks", resetCollections);
        if (resetPets != null) set.append("petSystem", resetPets);
        Document filter = new Document("_id", playerUuid.toString()).append("prestige.activeRun.runId", expectedRunId);
        if (policy.crystalCost > 0) filter.append("crystals", new Document("$gte", policy.crystalCost));
        Document update = new Document("$set", set);
        if (policy.crystalCost > 0) update.append("$inc", new Document("crystals", -policy.crystalCost));
        UpdateResult result = connection.collection("players").updateOne(filter, update);
        if (result != null && result.getMatchedCount() > 0) return ResetCommitResult.APPLIED;

        Document latestPlayer = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        PrestigeState latest = latestPlayer == null ? new PrestigeState() : decode(latestPlayer.get("prestige", Document.class));
        if (expectedRunId != null && expectedRunId.equals(latest.lastResetRunId) && latest.activeRun == null) return ResetCommitResult.DUPLICATE;
        if (latest.activeRun != null && expectedRunId != null && expectedRunId.equals(latest.activeRun.runId)
                && latestPlayer != null && crystals(latestPlayer) < policy.crystalCost) return ResetCommitResult.INSUFFICIENT_CRYSTALS;
        return ResetCommitResult.STALE;
    }

    @SuppressWarnings("unchecked") public static List<Document> resetRuneRanks(Document player) {
        if (player == null) return null;
        List<Document> storedCollections = (List<Document>) player.get("decks");
        if (storedCollections == null) return null;
        List<Document> collections = new ArrayList<>();
        for (Document storedCollection : storedCollections) {
            Document collection = new Document(storedCollection);
            List<Document> storedRunes = (List<Document>) storedCollection.get("cards");
            if (storedRunes != null) {
                List<Document> resetRunes = new ArrayList<>();
                for (Document storedRune : storedRunes) {
                    Document rune = new Document(storedRune);
                    int rank = rune.getInteger("layer", RuneStorageRules.NORMAL_RANK);
                    if (rank < RuneStorageRules.ADVANCED_RANK) rune.put("layer", RuneStorageRules.NORMAL_RANK);
                    resetRunes.add(rune);
                }
                collection.put("cards", resetRunes);
            }
            collections.add(collection);
        }
        return collections;
    }

    private Document encode(PrestigeState state) {
        Document root = new Document("schemaVersion", SCHEMA_VERSION);
        if (state.lastResetRunId != null) root.append("lastResetRunId", state.lastResetRunId);
        if (state.activeRun != null) root.append("activeRun", encodeRun(state.activeRun));
        Document branches = new Document();
        for (PrestigeBranch branch : PrestigeBranch.values()) branches.append(branch.name(), encodeBranch(state.branch(branch)));
        root.append("branches", branches);
        return root;
    }

    private Document encodeRun(PrestigeState.RunState run) {
        Document modifiers = new Document();
        for (Map.Entry<String, BigDecimal> entry : run.runModifiers.entrySet()) modifiers.append(entry.getKey(), exact(entry.getValue()));
        Document purchases = new Document();
        for (Map.Entry<String, Integer> entry : run.runExchangePurchaseCounts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) purchases.append(entry.getKey(), entry.getValue());
        }
        return new Document("runId", run.runId).append("branch", run.activeBranch.name()).append("startedAt", run.startedAt)
                .append("progress", exact(run.branchProgress)).append("level", run.branchLevel)
                .append("reachedRanks", new ArrayList<>(run.reachedRanks))
                .append("runBranchCurrency", exact(run.runBranchCurrency))
                .append("masteryPendingFragments", exact(run.masteryPendingFragments))
                .append("petPendingFragments", exact(run.petPendingFragments))
                .append("runExchange", new Document("purchases", purchases))
                .append("runModifiers", modifiers);
    }

    private Document encodeBranch(PrestigeState.BranchState state) {
        Document meta = new Document();
        for (Map.Entry<String, Integer> entry : state.rankMetaLevels.entrySet()) meta.append(entry.getKey(), entry.getValue());
        return new Document("bestSavedProgress", exact(state.bestSavedProgress)).append("rankMetaLevels", meta).append("rankMetaLevelCap", state.rankMetaLevelCap)
                .append("masteryTier", state.masteryTier);
    }

    @SuppressWarnings("unchecked") private PrestigeState decode(Document root) {
        PrestigeState state = new PrestigeState();
        if (root == null) return state;
        state.lastResetRunId = root.getString("lastResetRunId");
        Document run = root.get("activeRun", Document.class);
        if (run != null) try {
            PrestigeState.RunState active = new PrestigeState.RunState();
            active.runId = run.getString("runId"); active.activeBranch = PrestigeBranch.valueOf(run.getString("branch"));
            Object started = run.get("startedAt"); active.startedAt = started instanceof Number ? ((Number) started).longValue() : 0L;
            active.branchProgress = decimal(run.get("progress")); active.branchLevel = run.getInteger("level", 0);
            List<String> ranks = (List<String>) run.get("reachedRanks"); if (ranks != null) active.reachedRanks.addAll(ranks);
            active.runBranchCurrency = decimal(run.get("runBranchCurrency"));
            active.masteryPendingFragments = decimal(run.get("masteryPendingFragments"));
            active.petPendingFragments = decimal(run.get("petPendingFragments"));
            Document exchange = run.get("runExchange", Document.class);
            Document purchases = exchange == null ? null : exchange.get("purchases", Document.class);
            if (purchases != null) for (String key : purchases.keySet()) {
                Object value = purchases.get(key);
                if (value instanceof Number) active.runExchangePurchaseCounts.put(key, Math.max(0, ((Number) value).intValue()));
            }
            Document modifiers = run.get("runModifiers", Document.class);
            if (modifiers != null) for (String key : modifiers.keySet()) active.runModifiers.put(key, decimal(modifiers.get(key)));
            if (active.runId != null) state.activeRun = active;
        } catch (RuntimeException ignored) { state.activeRun = null; }
        Document branches = root.get("branches", Document.class);
        if (branches != null) for (PrestigeBranch branch : PrestigeBranch.values()) {
            Document stored = branches.get(branch.name(), Document.class); if (stored == null) continue;
            PrestigeState.BranchState target = state.branch(branch); target.bestSavedProgress = decimal(stored.get("bestSavedProgress"));
            target.rankMetaLevelCap = Math.max(0, stored.getInteger("rankMetaLevelCap", target.rankMetaLevelCap));
            target.masteryTier = Math.max(0, stored.getInteger("masteryTier", 0));
            Document meta = stored.get("rankMetaLevels", Document.class);
            if (meta != null) for (String key : meta.keySet()) { Object value = meta.get(key); if (value instanceof Number) target.rankMetaLevels.put(key, Math.max(0, ((Number) value).intValue())); }
        }
        return state;
    }

    private static int crystals(Document player) {
        Object raw = player == null ? null : player.get("crystals");
        if (raw == null) return 0;
        try {
            BigDecimal value = new BigDecimal(String.valueOf(raw));
            if (value.signum() <= 0) return 0;
            if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
            return value.intValue();
        } catch (RuntimeException ignored) { return 0; }
    }

    private static String exact(BigDecimal value) { return value == null || value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); }
    private static BigDecimal decimal(Object value) { if (value == null) return BigDecimal.ZERO; try { return new BigDecimal(String.valueOf(value)); } catch (NumberFormatException ignored) { return BigDecimal.ZERO; } }
}
