package com.example.revolution.persistence;

import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipRepository;
import com.mongodb.client.model.UpdateOptions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bson.Document;

/** Backward-compatible God Worship state embedded in the existing players document. */
public final class MongoGodWorshipRepository implements GodWorshipRepository {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoGodWorshipRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override public GodWorshipModel.RunState loadRunState(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return new GodWorshipModel.RunState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new GodWorshipModel.RunState() : decodeRun(player.get("godWorshipRun", Document.class));
    }

    @Override public GodWorshipModel.PersistentState loadPersistentState(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return new GodWorshipModel.PersistentState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new GodWorshipModel.PersistentState() : decodePersistent(player.get("godWorshipPersistent", Document.class));
    }

    @Override public void saveRunState(UUID playerUuid, GodWorshipModel.RunState state) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("godWorshipRun", encodeRun(state))), new UpdateOptions().upsert(true));
    }

    @Override public void savePersistentState(UUID playerUuid, GodWorshipModel.PersistentState state) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("godWorshipPersistent", encodePersistent(state))), new UpdateOptions().upsert(true));
    }

    @Override public void resetRunState(UUID playerUuid) {
        if (!enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("godWorshipRun", encodeRun(new GodWorshipModel.RunState()))),
                new UpdateOptions().upsert(true));
    }

    static Document encodeRun(GodWorshipModel.RunState state) {
        List<Document> gods = new ArrayList<>();
        for (GodWorshipModel.GodId id : GodWorshipModel.GodId.values()) {
            GodWorshipModel.State god = state.gods.get(id);
            if (god == null) god = new GodWorshipModel.State();
            gods.add(new Document("godId", id.name())
                    .append("unlocked", god.unlocked)
                    .append("level", Math.max(0L, god.level))
                    .append("phase", god.phase == null ? GodWorshipModel.Phase.LOCKED.name() : god.phase.name())
                    .append("buffActiveUntil", Math.max(0L, god.buffActiveUntilMillis))
                    .append("cooldownUntil", Math.max(0L, god.cooldownUntilMillis))
                    .append("chargeProgress", Math.max(0L, god.chargeProgress))
                    .append("offeringSettledUntil", Math.max(0L, god.offeringSettledUntilMillis)));
        }
        return new Document("schemaVersion", SCHEMA_VERSION).append("gods", gods);
    }

    @SuppressWarnings("unchecked")
    static GodWorshipModel.RunState decodeRun(Document root) {
        GodWorshipModel.RunState state = new GodWorshipModel.RunState();
        if (root == null) return state;
        List<Document> gods = (List<Document>) root.get("gods");
        if (gods == null) return state;
        for (Document stored : gods) {
            try {
                GodWorshipModel.GodId id = GodWorshipModel.GodId.valueOf(stored.getString("godId"));
                GodWorshipModel.State god = state.gods.get(id);
                god.unlocked = Boolean.TRUE.equals(stored.getBoolean("unlocked"));
                god.level = god.unlocked ? Math.max(1L, number(stored.get("level"))) : 0L;
                god.phase = god.unlocked
                        ? GodWorshipModel.Phase.valueOf(stored.getString("phase"))
                        : GodWorshipModel.Phase.LOCKED;
                god.buffActiveUntilMillis = Math.max(0L, number(stored.get("buffActiveUntil")));
                god.cooldownUntilMillis = Math.max(0L, number(stored.get("cooldownUntil")));
                god.chargeProgress = Math.max(0L, number(stored.get("chargeProgress")));
                god.offeringSettledUntilMillis = Math.max(0L, number(stored.get("offeringSettledUntil")));
            } catch (RuntimeException ignored) { }
        }
        return state;
    }

    static Document encodePersistent(GodWorshipModel.PersistentState state) {
        List<Document> preferences = new ArrayList<>();
        for (GodWorshipModel.GodId id : GodWorshipModel.GodId.values()) {
            GodWorshipModel.Preference preference = state.preferences.get(id);
            if (preference == null) preference = new GodWorshipModel.Preference();
            preferences.add(new Document("godId", id.name())
                    .append("autoWorshipEnabled", preference.autoWorshipEnabled)
                    .append("activationMode", preference.activationMode == null
                            ? GodWorshipModel.ActivationMode.MANUAL.name() : preference.activationMode.name())
                    .append("linkedGodId", preference.linkedGodId == null ? null : preference.linkedGodId.name()));
        }
        return new Document("schemaVersion", SCHEMA_VERSION)
                .append("offerings", exact(state.offerings))
                .append("permanentDurationLevel", Math.max(0L, state.permanentDurationLevel))
                .append("preferences", preferences);
    }

    @SuppressWarnings("unchecked")
    static GodWorshipModel.PersistentState decodePersistent(Document root) {
        GodWorshipModel.PersistentState state = new GodWorshipModel.PersistentState();
        if (root == null) return state;
        state.offerings = nonNegative(root.get("offerings"));
        state.permanentDurationLevel = Math.max(0L, number(root.get("permanentDurationLevel")));
        List<Document> preferences = (List<Document>) root.get("preferences");
        if (preferences == null) return state;
        for (Document stored : preferences) {
            try {
                GodWorshipModel.GodId id = GodWorshipModel.GodId.valueOf(stored.getString("godId"));
                GodWorshipModel.Preference preference = state.preferences.get(id);
                preference.autoWorshipEnabled = Boolean.TRUE.equals(stored.getBoolean("autoWorshipEnabled"));
                preference.activationMode = GodWorshipModel.ActivationMode.valueOf(stored.getString("activationMode"));
                String linked = stored.getString("linkedGodId");
                preference.linkedGodId = linked == null ? null : GodWorshipModel.GodId.valueOf(linked);
                try { preference.validateFor(id); } catch (IllegalArgumentException invalid) {
                    preference.activationMode = GodWorshipModel.ActivationMode.MANUAL;
                    preference.linkedGodId = null;
                }
            } catch (RuntimeException ignored) { }
        }
        return state;
    }

    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
    private static BigDecimal nonNegative(Object value) {
        try { return new BigDecimal(String.valueOf(value)).max(BigDecimal.ZERO); }
        catch (RuntimeException ignored) { return BigDecimal.ZERO; }
    }
    private static String exact(BigDecimal value) {
        return value == null || value.signum() <= 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
