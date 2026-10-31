package com.example.revolution.persistence;

import com.example.revolution.aura.AuraModel;
import com.example.revolution.aura.AuraRepository;
import com.example.revolution.effect.EffectResolver;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible v2.9 Aura state embedded in the existing players document. */
public final class MongoAuraRepository implements AuraRepository {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoAuraRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection; this.enabled = enabled;
    }

    @Override public AuraModel.PlayerState load(UUID playerUuid) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return new AuraModel.PlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        AuraModel.PlayerState state = player == null ? new AuraModel.PlayerState() : decode(player.get("auraState", Document.class));
        state.repairActiveReference(); return state;
    }

    @Override public void save(UUID playerUuid, AuraModel.PlayerState state) {
        if (playerUuid == null || state == null || !enabled || !connection.isConnected()) return;
        state.repairActiveReference();
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("auraState", encode(state))), new UpdateOptions().upsert(true));
    }

    static Document encode(AuraModel.PlayerState state) {
        List<Document> owned = new ArrayList<>();
        for (AuraModel.AuraInstance aura : state.ownedAuras) {
            List<Document> raw = new ArrayList<>();
            for (AuraModel.Roll roll : aura.rawRolls) raw.add(new Document("stat", roll.stat.name()).append("value", roll.additiveMultiplier.toPlainString()));
            List<Document> aggregated = new ArrayList<>();
            for (Map.Entry<EffectResolver.Stat, BigDecimal> effect : aura.aggregatedEffects.entrySet()) {
                aggregated.add(new Document("stat", effect.getKey().name()).append("value", effect.getValue().toPlainString()));
            }
            owned.add(new Document("instanceId", aura.instanceId.toString())
                    .append("rarity", aura.rarity.name()).append("origin", aura.origin.name())
                    .append("generationSeed", aura.generationSeed).append("rawRolls", raw)
                    .append("aggregatedEffects", aggregated)
                    .append("pinned", aura.pinned).append("createdAt", aura.createdAtMillis));
        }
        return new Document("schemaVersion", SCHEMA_VERSION).append("ownedAuras", owned)
                .append("activeAuraId", state.activeAuraId == null ? null : state.activeAuraId.toString())
                .append("totalChestOpens", Math.max(0L, state.totalChestOpens))
                .append("totalChestAuraDrops", Math.max(0L, state.totalChestAuraDrops))
                .append("skipOpeningAnimation", state.skipOpeningAnimation);
    }

    @SuppressWarnings("unchecked")
    static AuraModel.PlayerState decode(Document root) {
        AuraModel.PlayerState state = new AuraModel.PlayerState(); if (root == null) return state;
        List<Document> owned = (List<Document>) root.get("ownedAuras");
        if (owned != null) for (Document stored : owned) {
            try {
                List<AuraModel.Roll> rolls = new ArrayList<>(); List<Document> raw = (List<Document>) stored.get("rawRolls");
                if (raw != null) for (Document roll : raw) {
                    EffectResolver.Stat stat = EffectResolver.Stat.valueOf(roll.getString("stat"));
                    BigDecimal value = new BigDecimal(String.valueOf(roll.get("value"))); if (value.signum() >= 0) rolls.add(new AuraModel.Roll(stat, value));
                }
                AuraModel.AuraInstance aura = new AuraModel.AuraInstance(UUID.fromString(stored.getString("instanceId")),
                        AuraModel.Rarity.valueOf(stored.getString("rarity")), AuraModel.Origin.valueOf(stored.getString("origin")),
                        number(stored.get("generationSeed")), rolls, Boolean.TRUE.equals(stored.getBoolean("pinned")), number(stored.get("createdAt")));
                // Aggregates are persisted for audit but raw rolls remain canonical. Reject silently corrupted stored aggregates.
                List<Document> storedAggregated = (List<Document>) stored.get("aggregatedEffects");
                if (storedAggregated != null) for (Document effect : storedAggregated) {
                    EffectResolver.Stat stat = EffectResolver.Stat.valueOf(effect.getString("stat"));
                    BigDecimal value = new BigDecimal(String.valueOf(effect.get("value")));
                    BigDecimal recomputed = aura.aggregatedEffects.get(stat);
                    if (recomputed == null || recomputed.compareTo(value) != 0) throw new IllegalArgumentException("Aura aggregate audit mismatch");
                }
                state.ownedAuras.add(aura);
            } catch (RuntimeException ignored) { }
        }
        try { String active = root.getString("activeAuraId"); state.activeAuraId = active == null ? null : UUID.fromString(active); } catch (RuntimeException ignored) { state.activeAuraId = null; }
        state.totalChestOpens = Math.max(0L, number(root.get("totalChestOpens")));
        state.totalChestAuraDrops = Math.max(0L, number(root.get("totalChestAuraDrops")));
        state.skipOpeningAnimation = Boolean.TRUE.equals(root.getBoolean("skipOpeningAnimation"));
        state.repairActiveReference(); return state;
    }

    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
}
