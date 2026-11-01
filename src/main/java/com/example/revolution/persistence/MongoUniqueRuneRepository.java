package com.example.revolution.persistence;

import com.example.revolution.unique.UniqueRuneModel;
import com.example.revolution.unique.UniqueRuneRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;

/** v2.11.0 permanent Unique Rune state embedded in the existing players document. */
public final class MongoUniqueRuneRepository implements UniqueRuneRepository {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoUniqueRuneRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection; this.enabled = enabled;
    }

    @Override public UniqueRuneModel.PlayerState load(UUID playerUuid) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return new UniqueRuneModel.PlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        if (player == null) return new UniqueRuneModel.PlayerState();
        return decode(player.get("uniqueRuneState", Document.class));
    }

    @Override public void save(UUID playerUuid, UniqueRuneModel.PlayerState state) {
        if (playerUuid == null || state == null || !enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("uniqueRuneState", encode(state))), new UpdateOptions().upsert(true));
    }

    static Document encode(UniqueRuneModel.PlayerState state) {
        Document collections = new Document();
        for (UniqueRuneModel.CollectionId id : UniqueRuneModel.CollectionId.values()) {
            UniqueRuneModel.CollectionState collection = state.collection(id);
            List<Document> owned = new ArrayList<>();
            for (Map.Entry<String, UniqueRuneModel.Rarity> entry : collection.owned.entrySet())
                owned.add(new Document("runeId", entry.getKey()).append("rarity", entry.getValue().name()));
            List<Document> enhancement = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : collection.enhancement.entrySet())
                enhancement.add(new Document("runeId", entry.getKey()).append("level", Math.max(0, entry.getValue())));
            List<Document> rollbackOwned = new ArrayList<>();
            for (Map.Entry<String, UniqueRuneModel.Rarity> entry : collection.rollbackOwned.entrySet())
                rollbackOwned.add(new Document("runeId", entry.getKey()).append("rarity", entry.getValue().name()));
            collections.append(id.name(), new Document("currentRarity", collection.currentRarity.name())
                    .append("owned", owned).append("enhancement", enhancement)
                    .append("rollbackRarity", collection.rollbackRarity == null ? null : collection.rollbackRarity.name())
                    .append("rollbackOwned", rollbackOwned));
        }
        return new Document("schemaVersion", SCHEMA_VERSION).append("collections", collections)
                .append("lastDailyClaimDay", state.lastDailyClaimDay);
    }

    @SuppressWarnings("unchecked")
    static UniqueRuneModel.PlayerState decode(Document root) {
        UniqueRuneModel.PlayerState state = new UniqueRuneModel.PlayerState();
        if (root == null) return state; // v2.10.0 migration: COMMON, empty, no claim.
        Document collections = root.get("collections", Document.class);
        if (collections != null) for (UniqueRuneModel.CollectionId id : UniqueRuneModel.CollectionId.values()) {
            Document stored = collections.get(id.name(), Document.class); if (stored == null) continue;
            UniqueRuneModel.CollectionState target = state.collection(id);
            try { target.currentRarity = UniqueRuneModel.Rarity.valueOf(stored.getString("currentRarity")); }
            catch (RuntimeException ignored) { target.currentRarity = UniqueRuneModel.Rarity.COMMON; }
            if (!target.currentRarity.gameplayEnabled) target.currentRarity = UniqueRuneModel.Rarity.MYTHIC;
            List<Document> owned = (List<Document>) stored.get("owned");
            if (owned != null) for (Document item : owned) try {
                String runeId = item.getString("runeId"); UniqueRuneModel.Rarity rarity = UniqueRuneModel.Rarity.valueOf(item.getString("rarity"));
                if (runeId != null && rarity.gameplayEnabled) target.owned.put(runeId, rarity);
            } catch (RuntimeException ignored) { }
            List<Document> enhancement = (List<Document>) stored.get("enhancement");
            if (enhancement != null) for (Document item : enhancement) {
                String runeId = item.getString("runeId"); Object level = item.get("level");
                if (runeId != null && level instanceof Number) target.enhancement.put(runeId, Math.max(0, ((Number) level).intValue()));
            }
            try { String rollback = stored.getString("rollbackRarity"); target.rollbackRarity = rollback == null ? null : UniqueRuneModel.Rarity.valueOf(rollback); }
            catch (RuntimeException ignored) { target.rollbackRarity = null; }
            List<Document> rollbackOwned = (List<Document>) stored.get("rollbackOwned");
            if (rollbackOwned != null) for (Document item : rollbackOwned) try {
                String runeId = item.getString("runeId"); UniqueRuneModel.Rarity rarity = UniqueRuneModel.Rarity.valueOf(item.getString("rarity"));
                if (runeId != null && rarity.gameplayEnabled) target.rollbackOwned.put(runeId, rarity);
            } catch (RuntimeException ignored) { }
        }
        Object lastDaily = root.get("lastDailyClaimDay");
        state.lastDailyClaimDay = lastDaily instanceof Number ? ((Number) lastDaily).longValue() : Long.MIN_VALUE;
        return state;
    }
}
