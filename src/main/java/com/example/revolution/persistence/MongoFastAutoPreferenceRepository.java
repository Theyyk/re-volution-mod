package com.example.revolution.persistence;

import com.example.revolution.location.repository.FastAutoPreferenceRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.UUID;
import org.bson.Document;

/**
 * Fast-auto is the player-level /stage auto preference.
 * The legacy fastAutoEnabled field is only a fallback for existing DEV data.
 */
public final class MongoFastAutoPreferenceRepository implements FastAutoPreferenceRepository {
    private static final String COLLECTION = "players";
    private static final String AUTO_FIELD = "autoTransitionEnabled";
    private static final String LEGACY_FIELD = "fastAutoEnabled";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoFastAutoPreferenceRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override
    public boolean isFastAutoEnabled(UUID playerUuid) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return false;
        Document doc = connection.collection(COLLECTION)
                .find(new Document("_id", playerUuid.toString())).first();
        if (doc == null) return false;
        Boolean unified = doc.getBoolean(AUTO_FIELD);
        return unified != null ? unified : doc.getBoolean(LEGACY_FIELD, false);
    }

    @Override
    public void setFastAutoEnabled(UUID playerUuid, boolean enabledValue) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return;
        connection.collection(COLLECTION).updateOne(
                new Document("_id", playerUuid.toString()),
                new Document("$set", new Document(AUTO_FIELD, enabledValue)),
                new UpdateOptions().upsert(true));
    }
}
