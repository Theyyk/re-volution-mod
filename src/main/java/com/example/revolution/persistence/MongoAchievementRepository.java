package com.example.revolution.persistence;

import com.example.revolution.achievement.AchievementAtomicClaimStore;
import com.example.revolution.achievement.AchievementPlayerState;
import com.example.revolution.achievement.AchievementRepository;
import com.example.revolution.achievement.AchievementStage;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible v2.10.0 state embedded in the existing player document. */
public final class MongoAchievementRepository implements AchievementRepository, AchievementAtomicClaimStore {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;
    private final MongoAchievementAtomicClaimStore atomicClaims;

    public MongoAchievementRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
        this.atomicClaims = new MongoAchievementAtomicClaimStore(connection, enabled);
    }

    @Override public boolean available() { return atomicClaims.available(); }

    @Override
    public AchievementPlayerState load(UUID playerUuid) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return new AchievementPlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        if (player == null) return new AchievementPlayerState();
        Document stored = player.get("achievementState", Document.class);
        if (stored != null) return decode(stored);

        AchievementPlayerState migrated = migrateFromV29(player);
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("achievementState", encode(migrated))));
        return migrated;
    }

    @Override
    public void save(UUID playerUuid, AchievementPlayerState state) {
        if (playerUuid == null || state == null || !enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("achievementState", encode(state))), new UpdateOptions().upsert(true));
    }

    @Override
    public boolean commit(UUID playerUuid, String achievementId, int expectedPriorClaimedStage,
                          AchievementStage stage, AchievementPlayerState nextState) {
        return atomicClaims.commit(playerUuid, achievementId, expectedPriorClaimedStage, stage, nextState);
    }

    /** Only facts with exact v2.9 lifetime semantics are bootstrapped; unrecoverable history stays zero. */
    static AchievementPlayerState migrateFromV29(Document player) {
        AchievementPlayerState state = new AchievementPlayerState();
        if (player == null) return state;
        Document aura = player.get("auraState", Document.class);
        long chestDrops = aura == null ? 0L : Math.max(0L, number(aura.get("totalChestAuraDrops")));
        if (chestDrops > 0L) state.achievementProgress.put("aura_drops", BigDecimal.valueOf(chestDrops));
        return state;
    }

    static Document encode(AchievementPlayerState state) {
        Document progress = new Document();
        for (Map.Entry<String, BigDecimal> entry : state.achievementProgress.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue().signum() >= 0)
                progress.put(entry.getKey(), exact(entry.getValue()));
        }
        Document claimed = new Document();
        for (Map.Entry<String, Integer> entry : state.highestClaimedStage.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
                claimed.put(entry.getKey(), entry.getValue());
        }
        return new Document("schemaVersion", SCHEMA_VERSION)
                .append("progress", progress)
                .append("claimedStages", claimed)
                .append("everSelectedMasteryIds", new java.util.ArrayList<>(state.everSelectedMasteryIds))
                .append("everObtainedPetIds", new java.util.ArrayList<>(state.everObtainedPetIds))
                .append("visitedLocationIds", new java.util.ArrayList<>(state.visitedLocationIds))
                .append("revealedHiddenAchievements", new java.util.ArrayList<>(state.revealedHiddenAchievements))
                .append("achievementPoints", Math.max(0L, state.achievementPoints));
    }

    @SuppressWarnings("unchecked")
    static AchievementPlayerState decode(Document root) {
        AchievementPlayerState state = new AchievementPlayerState();
        if (root == null) return state;
        Document progress = root.get("progress", Document.class);
        if (progress != null) for (Map.Entry<String, Object> entry : progress.entrySet()) {
            BigDecimal value = decimal(entry.getValue());
            if (value.signum() >= 0) state.achievementProgress.put(entry.getKey(), value);
        }
        Document claimed = root.get("claimedStages", Document.class);
        if (claimed != null) for (Map.Entry<String, Object> entry : claimed.entrySet()) {
            int value = integer(entry.getValue());
            if (value > 0) state.highestClaimedStage.put(entry.getKey(), value);
        }
        addStrings(state.everSelectedMasteryIds, (List<Object>) root.get("everSelectedMasteryIds"));
        addStrings(state.everObtainedPetIds, (List<Object>) root.get("everObtainedPetIds"));
        addStrings(state.visitedLocationIds, (List<Object>) root.get("visitedLocationIds"));
        addStrings(state.revealedHiddenAchievements, (List<Object>) root.get("revealedHiddenAchievements"));
        state.achievementPoints = Math.max(0L, number(root.get("achievementPoints")));
        return state;
    }

    private static void addStrings(Set<String> target, List<Object> stored) {
        if (stored == null) return;
        for (Object value : stored) if (value != null && !String.valueOf(value).trim().isEmpty()) target.add(String.valueOf(value));
    }

    private static BigDecimal decimal(Object value) {
        try { return new BigDecimal(String.valueOf(value)).max(BigDecimal.ZERO); }
        catch (RuntimeException ignored) { return BigDecimal.ZERO; }
    }
    private static int integer(Object value) { return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0; }
    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
    private static String exact(BigDecimal value) {
        return value == null || value.signum() <= 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
