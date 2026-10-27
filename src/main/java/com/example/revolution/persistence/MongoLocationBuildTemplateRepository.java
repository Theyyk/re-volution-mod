package com.example.revolution.persistence;

import com.example.revolution.location.repository.LocationBuildTemplateRepository;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

/** Stores links only; REBuilder owns the template format and files. */
public final class MongoLocationBuildTemplateRepository implements LocationBuildTemplateRepository {
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoLocationBuildTemplateRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    private void requireAvailable() {
        if (!enabled || !connection.isConnected()) throw new IllegalStateException("Build template storage is unavailable");
    }

    @Override public String get(int worldLocationId) {
        requireAvailable();
        Document doc = connection.collection("location_build_templates")
                .find(new Document("worldLocationId", worldLocationId)).first();
        return doc == null ? null : doc.getString("templateName");
    }

    @Override public void upsert(int worldLocationId, String templateName) {
        requireAvailable();
        Document filter = new Document("worldLocationId", worldLocationId);
        connection.collection("location_build_templates").updateOne(filter,
                new Document("$set", new Document("worldLocationId", worldLocationId).append("templateName", templateName)),
                new UpdateOptions().upsert(true));
    }
}
