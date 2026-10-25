package com.example.revolution.persistence;

import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.repository.LocationGenerationRepository;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

/** Persists reproducible physical generation provenance separately from gameplay geometry. */
public final class MongoLocationGenerationRepository implements LocationGenerationRepository {
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoLocationGenerationRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    @Override public void requireAvailable() {
        if (!enabled || !connection.isConnected()) throw new IllegalStateException("Generation storage is unavailable");
        connection.database().runCommand(new Document("ping", 1));
    }

    @Override public void save(LocationGenerationPlan plan) {
        requireAvailable();
        connection.collection("location_generations").updateOne(
                new Document("locationId", plan.getInternalLocationId()),
                new Document("$set", toDocument(plan)), new UpdateOptions().upsert(true));
    }

    @Override public void delete(int internalLocationId) {
        requireAvailable();
        connection.collection("location_generations").deleteOne(new Document("locationId", internalLocationId));
    }

    static Document toDocument(LocationGenerationPlan plan) {
        return new Document("locationId", plan.getInternalLocationId())
                .append("worldLocationId", plan.getWorldLocationId()).append("mobSlotId", plan.getMobSlotId())
                .append("templateName", plan.getTemplateId()).append("seed", plan.getSeed())
                .append("rotationDegrees", plan.getRotationDegrees())
                .append("anchorX", plan.getCenterX()).append("anchorY", plan.getCenterY()).append("anchorZ", plan.getCenterZ());
    }
}
