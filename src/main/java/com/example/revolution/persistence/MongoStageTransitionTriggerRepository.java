package com.example.revolution.persistence;

import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

/** Mongo adapter for configurable stage boundary rings and notification text. */
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
    public StageTransitionTriggerDefinition get(int worldLocationId, int sourceMobSlotId) {
        if (!enabled || !connection.isConnected()) return null;
        return fromDocument(collection().find(filter(worldLocationId, sourceMobSlotId)).first());
    }

    @Override
    public void upsert(StageTransitionTriggerDefinition trigger) {
        if (!enabled || !connection.isConnected() || trigger == null) return;
        Document values = new Document("worldLocationId", trigger.getWorldLocationId())
                .append("mobSlotId", trigger.getSourceMobSlotId())
                .append("offset", trigger.getOffset())
                .append("radius", trigger.getRadius())
                .append("displayMode", trigger.getDisplayMode().name());
        if (trigger.getLockedMessage() != null) values.append("lockedMessage", trigger.getLockedMessage());
        if (trigger.getForwardMessage() != null) values.append("forwardMessage", trigger.getForwardMessage());
        if (trigger.getBackwardMessage() != null) values.append("backwardMessage", trigger.getBackwardMessage());

        Document unset = new Document("width", "")
                .append("centerX", "")
                .append("centerY", "")
                .append("centerZ", "")
                .append("halfHeight", "")
                .append("enteringMessage", "");
        if (trigger.getLockedMessage() == null) unset.append("lockedMessage", "");
        if (trigger.getForwardMessage() == null) unset.append("forwardMessage", "");
        if (trigger.getBackwardMessage() == null) unset.append("backwardMessage", "");
        collection().updateOne(
                filter(trigger.getWorldLocationId(), trigger.getSourceMobSlotId()),
                new Document("$set", values).append("$unset", unset),
                new UpdateOptions().upsert(true));
    }

    @Override
    public void delete(int worldLocationId, int sourceMobSlotId) {
        if (!enabled || !connection.isConnected()) return;
        collection().deleteOne(filter(worldLocationId, sourceMobSlotId));
    }

    private Document filter(int worldLocationId, int sourceMobSlotId) {
        return new Document("worldLocationId", worldLocationId).append("mobSlotId", sourceMobSlotId);
    }

    private StageTransitionTriggerDefinition fromDocument(Document doc) {
        if (doc == null) return null;
        try {
            Integer worldLocationId = doc.getInteger("worldLocationId");
            Integer mobSlotId = doc.getInteger("mobSlotId");
            Object offset = doc.get("offset");
            Object radius = doc.get("radius");
            if (worldLocationId == null || mobSlotId == null) return null;

            // Backward compatibility for the short-lived width-only DEV schema.
            if (!(offset instanceof Number) || !(radius instanceof Number)) {
                Object width = doc.get("width");
                if (!(width instanceof Number)) return null;
                offset = 0.0D;
                radius = ((Number) width).doubleValue();
            }

            return new StageTransitionTriggerDefinition(
                    worldLocationId,
                    mobSlotId,
                    ((Number) offset).doubleValue(),
                    ((Number) radius).doubleValue(),
                    displayMode(doc.getString("displayMode")),
                    doc.getString("lockedMessage"),
                    doc.getString("forwardMessage"),
                    doc.getString("backwardMessage"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DisplayMode displayMode(String value) {
        if (value == null) return DisplayMode.ACTION_BAR;
        try { return DisplayMode.valueOf(value); }
        catch (IllegalArgumentException ignored) { return DisplayMode.ACTION_BAR; }
    }
}
