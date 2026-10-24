package com.example.revolution.persistence;

import com.example.revolution.location.repository.StageTransitionPreferenceRepository;
import com.mongodb.client.model.UpdateOptions;
import java.util.UUID;
import org.bson.Document;

/** Stores the global per-player auto-transition preference in the existing players document. */
public final class MongoStageTransitionPreferenceRepository implements StageTransitionPreferenceRepository {
    private static final String COLLECTION = "players";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoStageTransitionPreferenceRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override
    public boolean isAutoTransitionEnabled(UUID playerUuid) {
        if (playerUuid == null) return false;
        if (!enabled || !connection.isConnected()) return true;
        Document doc = connection.collection(COLLECTION)
                .find(new Document("_id", playerUuid.toString())).first();
        if (doc == null) return true;
        return doc.getBoolean("autoTransitionEnabled", true);
    }

    @Override
    public void setAutoTransitionEnabled(UUID playerUuid, boolean enabledValue) {
        if (playerUuid == null || !enabled || !connection.isConnected()) return;
        connection.collection(COLLECTION).updateOne(
                new Document("_id", playerUuid.toString()),
                new Document("$set", new Document("autoTransitionEnabled", enabledValue)),
                new UpdateOptions().upsert(true));
    }
}
