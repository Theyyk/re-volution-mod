package com.example.revolution.persistence;

import com.example.revolution.location.repository.FastAutoPreferenceRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.UUID;
import org.bson.Document;

/** Stores the independent fast-auto preference in the existing players document. */
public final class MongoFastAutoPreferenceRepository implements FastAutoPreferenceRepository {
    private static final String COLLECTION = "players";
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
        return doc != null && doc.getBoolean("fastAutoEnabled", false);
    }

    @Override
    public void setFastAutoEnabled(UUID playerUuid, boolean enabledValue) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return;
        connection.collection(COLLECTION).updateOne(
                new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("fastAutoEnabled", enabledValue)),
                new UpdateOptions().upsert(true));
    }
}
