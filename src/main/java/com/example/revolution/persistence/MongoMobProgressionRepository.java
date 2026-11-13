package com.example.revolution.persistence;

import com.example.revolution.mob.repository.MobProgressionRepository;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;

/** Mongo adapter for per-player mob-zone progression. */
public final class MongoMobProgressionRepository implements MobProgressionRepository {
    private static final String COLLECTION = "mob_progression";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoMobProgressionRepository(MongoConnection connection) { this(connection, true); }

    public MongoMobProgressionRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override
    public Set<Integer> getUnlockedSlots(UUID playerUuid, int worldLocationId) {
        if (!enabled || !connection.isConnected() || playerUuid == null || worldLocationId < 0) {
            return Collections.emptySet();
        }
        Document doc = connection.collection(COLLECTION).find(new Document("_id", key(playerUuid, worldLocationId))).first();
        if (doc == null) return Collections.emptySet();
        return Collections.unmodifiableSet(visibleSlots(doc));
    }

    @Override
    public boolean unlockSlot(UUID playerUuid, int worldLocationId, int mobSlotId) {
        if (!valid(playerUuid, worldLocationId) || mobSlotId < 1 || mobSlotId > 99) return false;
        Document identity = new Document("playerUuid", playerUuid.toString()).append("locationId", worldLocationId);
        Document update = new Document("$setOnInsert", identity)
                .append("$addToSet", new Document("permanentSlots", mobSlotId));
        UpdateResult result = connection.collection(COLLECTION).updateOne(
                new Document("_id", key(playerUuid, worldLocationId)), update, new UpdateOptions().upsert(true));
        return result != null && (result.getModifiedCount() > 0 || result.getUpsertedId() != null);
    }

    @Override
    public boolean grantPermanentSlots(UUID playerUuid, int worldLocationId, Set<Integer> slots) {
        if (!valid(playerUuid, worldLocationId) || slots == null || slots.isEmpty()) return false;
        List<Integer> sanitized = new ArrayList<>();
        for (Integer slot : slots) {
            if (slot == null || slot < 1 || slot > 99) return false;
            sanitized.add(slot);
        }
        Document identity = new Document("playerUuid", playerUuid.toString()).append("locationId", worldLocationId);
        // A single additive update: no $set on unlockedSlots, no $unset of legacyRunSlots.
        Document update = new Document("$setOnInsert", identity)
                .append("$addToSet", new Document("permanentSlots", new Document("$each", sanitized)));
        UpdateResult result = connection.collection(COLLECTION).updateOne(
                new Document("_id", key(playerUuid, worldLocationId)), update, new UpdateOptions().upsert(true));
        return result != null && result.wasAcknowledged();
    }

    @Override
    public boolean replaceUnlockedSlots(UUID playerUuid, int worldLocationId, Set<Integer> unlockedSlots) {
        if (!valid(playerUuid, worldLocationId) || unlockedSlots == null) return false;
        List<Integer> sanitized = new ArrayList<>();
        for (Integer slot : unlockedSlots) {
            if (slot != null && slot >= 1 && slot <= 99) sanitized.add(slot);
        }
        if (sanitized.isEmpty()) {
            connection.collection(COLLECTION).deleteOne(new Document("_id", key(playerUuid, worldLocationId)));
            return true;
        }
        Document identity = new Document("playerUuid", playerUuid.toString()).append("locationId", worldLocationId);
        Document update = new Document("$setOnInsert", identity)
                .append("$set", new Document("unlockedSlots", sanitized))
                .append("$unset", new Document("legacyRunSlots", "").append("permanentSlots", ""));
        connection.collection(COLLECTION).updateOne(
                new Document("_id", key(playerUuid, worldLocationId)), update, new UpdateOptions().upsert(true));
        return true;
    }

    @Override
    public boolean clearFromLocation(UUID playerUuid, int worldLocationId) {
        if (!valid(playerUuid, worldLocationId)) return false;
        Document filter = new Document("playerUuid", playerUuid.toString())
                .append("locationId", new Document("$gte", worldLocationId));
        connection.collection(COLLECTION).deleteMany(filter);
        return true;
    }

    /**
     * Legacy data has no provenance. Only an explicit admin-confirmed, player-scoped
     * migration may mask these historical grants. The underlying slots remain intact.
     * Repeated classification is a no-op; fresh permanent grants stay effective.
     */
    @Override public boolean classifyLegacyRunSlots(UUID playerUuid, int worldLocationId) {
        if (!valid(playerUuid, worldLocationId)) return false;
        Document key = new Document("_id", key(playerUuid, worldLocationId));
        Document current = connection.collection(COLLECTION).find(key).first();
        if (current == null || current.containsKey("legacyRunSlots")) return true;
        Set<Integer> legacy = stageSlots(current.get("unlockedSlots"));
        if (legacy.isEmpty()) return true;
        Document filter = new Document("_id", key(playerUuid, worldLocationId))
                .append("legacyRunSlots", new Document("$exists", false));
        UpdateResult result = connection.collection(COLLECTION).updateOne(filter,
                new Document("$set", new Document("legacyRunSlots", new ArrayList<>(legacy))));
        if (result != null && result.getMatchedCount() == 1) return true;
        Document latest = connection.collection(COLLECTION).find(key).first();
        return latest != null && latest.containsKey("legacyRunSlots");
    }

    /** Pure, backward-compatible read model: old rows retain full access until explicitly classified. */
    static Set<Integer> visibleSlots(Document doc) {
        Set<Integer> result = stageSlots(doc.get("unlockedSlots"));
        result.removeAll(stageSlots(doc.get("legacyRunSlots")));
        result.addAll(stageSlots(doc.get("permanentSlots")));
        return result;
    }

    private static Set<Integer> stageSlots(Object raw) {
        Set<Integer> result = new LinkedHashSet<>();
        if (raw instanceof List) for (Object value : (List<?>) raw) {
            if (value instanceof Number) {
                int slot = ((Number) value).intValue();
                if (slot >= 1 && slot <= 99) result.add(slot);
            }
        }
        return result;
    }

    private boolean valid(UUID playerUuid, int worldLocationId) {
        return enabled && connection.isConnected() && playerUuid != null && worldLocationId >= 0;
    }

    private String key(UUID playerUuid, int worldLocationId) {
        return playerUuid.toString() + ":" + worldLocationId;
    }
}
