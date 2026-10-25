package com.example.revolution.persistence;

import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

/** Mongo adapter for per-Location gameplay stage geometry templates. */
public final class MongoLocationStageTemplateRepository implements LocationStageTemplateRepository {
    private static final String COLLECTION = "location_stage_templates";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoLocationStageTemplateRepository(MongoConnection connection, boolean enabled) {
        if (connection == null) throw new IllegalArgumentException("connection is required");
        this.connection = connection;
        this.enabled = enabled;
    }

    private MongoCollection<Document> collection() { return connection.collection(COLLECTION); }

    @Override
    public LocationStageTemplate get(int worldLocationId) {
        if (!enabled || !connection.isConnected()) return null;
        return fromDocument(collection().find(new Document("worldLocationId", worldLocationId)).first());
    }

    @Override
    public void upsert(LocationStageTemplate template) {
        if (!enabled || !connection.isConnected() || template == null) return;
        Document values = new Document("worldLocationId", template.getWorldLocationId())
                .append("mobRadius", template.getMobRadius())
                .append("triggerOffset", template.getTriggerOffset())
                .append("triggerRadius", template.getTriggerRadius());
        collection().updateOne(
                new Document("worldLocationId", template.getWorldLocationId()),
                new Document("$set", values),
                new UpdateOptions().upsert(true));
    }

    @Override
    public void delete(int worldLocationId) {
        if (!enabled || !connection.isConnected()) return;
        collection().deleteOne(new Document("worldLocationId", worldLocationId));
    }

    private LocationStageTemplate fromDocument(Document doc) {
        if (doc == null) return null;
        try {
            Integer worldLocationId = doc.getInteger("worldLocationId");
            Object mobRadius = doc.get("mobRadius");
            Object triggerOffset = doc.get("triggerOffset");
            Object triggerRadius = doc.get("triggerRadius");
            if (worldLocationId == null || !(mobRadius instanceof Number)
                    || !(triggerOffset instanceof Number) || !(triggerRadius instanceof Number)) return null;
            return new LocationStageTemplate(
                    worldLocationId,
                    ((Number) mobRadius).doubleValue(),
                    ((Number) triggerOffset).doubleValue(),
                    ((Number) triggerRadius).doubleValue());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
