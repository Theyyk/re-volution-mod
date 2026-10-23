package com.example.revolution.persistence;

import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

/** Mongo adapter for configurable stage transition trigger zones and notification text. */
public final class MongoStageTransitionTriggerRepository implements StageTransitionTriggerRepository {
    private static final String COLLECTION = "stage_transition_triggers";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoStageTransitionTriggerRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    private MongoCollection<Document> collection() { return connection.collection(COLLECTION); }

    @Override
    public List<StageTransitionTriggerDefinition> getAll() {
        List<StageTransitionTriggerDefinition> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : collection().find()) {
            StageTransitionTriggerDefinition trigger = fromDocument(doc);
            if (trigger != null) result.add(trigger);
        }
        return result;
    }

    @Override
    public StageTransitionTriggerDefinition get(int worldLocationId, int mobSlotId) {
        if (!enabled || !connection.isConnected()) return null;
        return fromDocument(collection().find(filter(worldLocationId, mobSlotId)).first());
    }

    @Override
    public void upsert(StageTransitionTriggerDefinition trigger) {
        if (!enabled || !connection.isConnected() || trigger == null) return;
        Document values = new Document("worldLocationId", trigger.getWorldLocationId())
                .append("mobSlotId", trigger.getMobSlotId())
                .append("centerX", trigger.getCenterX())
                .append("centerY", trigger.getCenterY())
                .append("centerZ", trigger.getCenterZ())
                .append("radius", trigger.getRadius())
                .append("halfHeight", trigger.getHalfHeight())
                .append("displayMode", trigger.getDisplayMode().name());
        if (trigger.getLockedMessage() != null) values.append("lockedMessage", trigger.getLockedMessage());
        if (trigger.getEnteringMessage() != null) values.append("enteringMessage", trigger.getEnteringMessage());

        Document unset = new Document();
        if (trigger.getLockedMessage() == null) unset.append("lockedMessage", "");
        if (trigger.getEnteringMessage() == null) unset.append("enteringMessage", "");
        Document update = new Document("$set", values);
        if (!unset.isEmpty()) update.append("$unset", unset);
        collection().updateOne(filter(trigger.getWorldLocationId(), trigger.getMobSlotId()), update,
                new UpdateOptions().upsert(true));
    }

    @Override
    public void delete(int worldLocationId, int mobSlotId) {
        if (!enabled || !connection.isConnected()) return;
        collection().deleteOne(filter(worldLocationId, mobSlotId));
    }

    private Document filter(int worldLocationId, int mobSlotId) {
        return new Document("worldLocationId", worldLocationId).append("mobSlotId", mobSlotId);
    }

    private StageTransitionTriggerDefinition fromDocument(Document doc) {
        if (doc == null) return null;
        try {
            Integer worldLocationId = doc.getInteger("worldLocationId");
            Integer mobSlotId = doc.getInteger("mobSlotId");
            if (worldLocationId == null || mobSlotId == null) return null;
            return new StageTransitionTriggerDefinition(
                    worldLocationId,
                    mobSlotId,
                    number(doc, "centerX", 0.0D),
                    number(doc, "centerY", 64.0D),
                    number(doc, "centerZ", 0.0D),
                    number(doc, "radius", 2.0D),
                    number(doc, "halfHeight", 2.0D),
                    displayMode(doc.getString("displayMode")),
                    doc.getString("lockedMessage"),
                    doc.getString("enteringMessage"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private double number(Document doc, String key, double fallback) {
        Object value = doc.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private DisplayMode displayMode(String value) {
        if (value == null) return DisplayMode.ACTION_BAR;
        try { return DisplayMode.valueOf(value); }
        catch (IllegalArgumentException ignored) { return DisplayMode.ACTION_BAR; }
    }
}
