package com.example.revolution.persistence;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

/** Mongo adapter for Location geometry, stage transition points, and one-entity-per-slot mob spawn configuration. */
public final class MongoLocationRepository implements LocationRepository {
    private static final String LOCATIONS = "custom_locations";
    private static final String SPAWNS = "custom_mob_spawns";
    private final MongoConnection connection;
    private final boolean enabled;

    public MongoLocationRepository(MongoConnection connection, boolean enabled) {
        this.connection = connection;
        this.enabled = enabled;
    }

    private MongoCollection<Document> locations() { return connection.collection(LOCATIONS); }
    private MongoCollection<Document> spawns() { return connection.collection(SPAWNS); }

    @Override
    public List<LocationDefinition> getLocations() {
        List<LocationDefinition> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : locations().find()) {
            LocationDefinition location = fromLocationDocument(doc);
            if (location != null) result.add(location);
        }
        return result;
    }

    @Override
    public LocationDefinition getLocation(int locationId) {
        if (!enabled || !connection.isConnected()) return null;
        Document doc = locations().find(new Document("locationId", locationId)).first();
        return fromLocationDocument(doc);
    }

    @Override
    public void upsertLocation(LocationDefinition location) {
        if (!enabled || !connection.isConnected() || location == null) return;
        Document filter = new Document("locationId", location.getLocationId());
        Document values = new Document("locationId", location.getLocationId())
                .append("name", location.getName())
                .append("centerX", location.getCenterX())
                .append("centerZ", location.getCenterZ())
                .append("radius", location.getRadius());
        if (location.hasTransitionPoint()) {
            values.append("transitionX", location.getTransitionX())
                    .append("transitionY", location.getTransitionY())
                    .append("transitionZ", location.getTransitionZ())
                    .append("transitionYaw", (double) location.getTransitionYaw())
                    .append("transitionPitch", (double) location.getTransitionPitch());
        }
        locations().updateOne(filter, new Document("$set", values), new UpdateOptions().upsert(true));
    }

    @Override
    public void deleteLocation(int locationId) {
        if (!enabled || !connection.isConnected()) return;
        locations().deleteOne(new Document("locationId", locationId));
    }

    @Override
    public List<StoredMobSpawn> getMobSpawns() {
        List<StoredMobSpawn> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : spawns().find()) {
            StoredMobSpawn spawn = fromDocument(doc);
            if (spawn != null) result.add(spawn);
        }
        return result;
    }

    @Override
    public List<StoredMobSpawn> getMobSpawns(int locationId) {
        List<StoredMobSpawn> result = new ArrayList<>();
        if (!enabled || !connection.isConnected()) return result;
        for (Document doc : spawns().find(new Document("locationId", locationId))) {
            StoredMobSpawn spawn = fromDocument(doc);
            if (spawn != null) result.add(spawn);
        }
        return result;
    }

    @Override
    public StoredMobSpawn getMobSpawn(int locationId, int spawnId) {
        if (!enabled || !connection.isConnected()) return null;
        Document doc = spawns().find(new Document("locationId", locationId).append("spawnId", spawnId)).first();
        return fromDocument(doc);
    }

    @Override
    public void upsertMobSpawn(StoredMobSpawn spawn) {
        if (!enabled || !connection.isConnected() || spawn == null) return;
        Document filter = new Document("locationId", spawn.getLocationId()).append("spawnId", spawn.getSpawnId());
        Document values = new Document("locationId", spawn.getLocationId())
                .append("spawnId", spawn.getSpawnId())
                .append("mobName", spawn.getMobName())
                .append("x", spawn.getSpawnX())
                .append("y", spawn.getSpawnY())
                .append("z", spawn.getSpawnZ())
                .append("yaw", (double) spawn.getYaw())
                .append("pitch", (double) spawn.getPitch())
                .append("spawnMode", spawn.getSpawnMode().name())
                .append("movementMode", spawn.getMovementMode().name())
                .append("realmScope", spawn.getRealmScope().name())
                .append("enabled", spawn.isEnabled());
        spawns().updateOne(filter, new Document("$set", values), new UpdateOptions().upsert(true));
    }

    @Override
    public void deleteMobSpawn(int locationId, int spawnId) {
        if (!enabled || !connection.isConnected()) return;
        spawns().deleteOne(new Document("locationId", locationId).append("spawnId", spawnId));
    }

    private LocationDefinition fromLocationDocument(Document doc) {
        if (doc == null) return null;
        try {
            Integer locationId = doc.getInteger("locationId");
            String name = doc.getString("name");
            if (locationId == null || name == null) return null;
            double centerX = number(doc, "centerX", 0.0D);
            double centerZ = number(doc, "centerZ", 0.0D);
            double radius = number(doc, "radius", 5.0D);
            if (hasNumber(doc, "transitionX") && hasNumber(doc, "transitionY") && hasNumber(doc, "transitionZ")) {
                return new LocationDefinition(
                        locationId,
                        name,
                        centerX,
                        centerZ,
                        radius,
                        number(doc, "transitionX", 0.0D),
                        number(doc, "transitionY", 64.0D),
                        number(doc, "transitionZ", 0.0D),
                        (float) number(doc, "transitionYaw", 0.0D),
                        (float) number(doc, "transitionPitch", 0.0D));
            }
            return new LocationDefinition(locationId, name, centerX, centerZ, radius);
        } catch (RuntimeException ignored) {
            // Invalid persisted entries are ignored; runtime validation reports missing references later.
            return null;
        }
    }

    private StoredMobSpawn fromDocument(Document doc) {
        if (doc == null) return null;
        try {
            Integer locationId = doc.getInteger("locationId");
            Integer spawnId = doc.getInteger("spawnId");
            String mobName = doc.getString("mobName");
            if (locationId == null || spawnId == null || mobName == null) return null;
            return new StoredMobSpawn(
                    locationId,
                    spawnId,
                    mobName,
                    number(doc, "x", 0.0D),
                    number(doc, "y", 64.0D),
                    number(doc, "z", 0.0D),
                    (float) number(doc, "yaw", 0.0D),
                    (float) number(doc, "pitch", 0.0D),
                    enumValue(SpawnMode.class, doc.getString("spawnMode"), SpawnMode.FIXED_POINT),
                    enumValue(MovementMode.class, doc.getString("movementMode"), MovementMode.STATIONARY),
                    enumValue(SpawnRealmScope.class, doc.getString("realmScope"), SpawnRealmScope.PLAYER_REALMS),
                    doc.getBoolean("enabled", true));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean hasNumber(Document doc, String key) {
        return doc.get(key) instanceof Number;
    }

    private double number(Document doc, String key, double fallback) {
        Object raw = doc.get(key);
        return raw instanceof Number ? ((Number) raw).doubleValue() : fallback;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        if (value == null) return fallback;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
