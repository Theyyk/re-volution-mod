package com.example.revolution.persistence;

import com.example.revolution.mechanic.MechanicUnlockModel;
import com.example.revolution.mechanic.MechanicUnlockRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.List;
import java.util.UUID;
import org.bson.Document;

/** v2.12.0 permanent mechanic unlock state embedded in the existing players document. */
public final class MongoMechanicUnlockRepository implements MechanicUnlockRepository {
    private static final int SCHEMA_VERSION = 1;
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoMechanicUnlockRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection; this.enabled = enabled;
    }

    @Override public MechanicUnlockModel.PlayerState load(UUID playerUuid) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return new MechanicUnlockModel.PlayerState();
        Document player = connection.collection("players").find(new Document("_id", playerUuid.toString())).first();
        return player == null ? new MechanicUnlockModel.PlayerState() : decode(player.get("mechanicUnlockState", Document.class));
    }

    @Override public void save(UUID playerUuid, MechanicUnlockModel.PlayerState state) {
        if (playerUuid == null || state == null || !enabled || !connection.isConnected()) return;
        connection.collection("players").updateOne(new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("mechanicUnlockState", encode(state))), new UpdateOptions().upsert(true));
    }

    static Document encode(MechanicUnlockModel.PlayerState state) {
        Document max = new Document();
        for (MechanicUnlockModel.Rarity rarity : MechanicUnlockModel.Rarity.values()) max.append(rarity.name(), state.maxProgress(rarity));
        return new Document("schemaVersion", SCHEMA_VERSION)
                .append("maxRuneProgressByRarity", max)
                .append("unlockedMechanicIds", new java.util.ArrayList<>(state.unlockedMechanicIds))
                .append("forceUnlockedMechanicIds", new java.util.ArrayList<>(state.forceUnlockedMechanicIds))
                .append("forceLockedMechanicIds", new java.util.ArrayList<>(state.forceLockedMechanicIds));
    }

    @SuppressWarnings("unchecked") static MechanicUnlockModel.PlayerState decode(Document root) {
        MechanicUnlockModel.PlayerState state = new MechanicUnlockModel.PlayerState();
        if (root == null) return state;
        Document max = root.get("maxRuneProgressByRarity", Document.class);
        if (max != null) for (MechanicUnlockModel.Rarity rarity : MechanicUnlockModel.Rarity.values()) {
            Object raw = max.get(rarity.name());
            if (raw instanceof Number) state.maxRuneProgressByRarity.put(rarity, Math.max(0, ((Number) raw).intValue()));
        }
        copyStrings((List<Object>) root.get("unlockedMechanicIds"), state.unlockedMechanicIds);
        copyStrings((List<Object>) root.get("forceUnlockedMechanicIds"), state.forceUnlockedMechanicIds);
        copyStrings((List<Object>) root.get("forceLockedMechanicIds"), state.forceLockedMechanicIds);
        return state;
    }

    private static void copyStrings(List<Object> source, java.util.Set<String> target) {
        if (source == null) return;
        for (Object item : source) if (item instanceof String && !((String) item).trim().isEmpty()) target.add((String) item);
    }
}
