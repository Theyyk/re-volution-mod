package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationCatalog;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobSpawnDefinition;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure lookup and geometry rules for Locations and Mob Spawns. Runtime spawning starts after v1.3.5. */
public final class LocationService {
    private final LocationCatalog catalog;

    public LocationService(LocationCatalog catalog) {
        if (catalog == null) throw new IllegalArgumentException("catalog is required");
        this.catalog = catalog;
    }

    public LocationDefinition requireLocation(int locationId) {
        LocationDefinition location = catalog.byLocationId(locationId);
        if (location == null) throw new IllegalArgumentException("Unknown Location ID: " + locationId);
        return location;
    }

    public MobSpawnDefinition requireSpawn(int locationId, int spawnId) {
        MobSpawnDefinition spawn = catalog.spawn(locationId, spawnId);
        if (spawn == null) {
            throw new IllegalArgumentException("Unknown Mob Spawn ID " + spawnId + " in Location " + locationId);
        }
        return spawn;
    }

    public boolean isInsideLocation(int locationId, double x, double z) {
        return requireLocation(locationId).contains(x, z);
    }

    public boolean isBeyondLocation(int locationId, double x, double z, double margin) {
        if (!Double.isFinite(margin) || margin < 0.0D) throw new IllegalArgumentException("margin must be non-negative and finite");
        LocationDefinition location = requireLocation(locationId);
        double dx = x - location.getCenterX();
        double dz = z - location.getCenterZ();
        double hardRadius = location.getRadius() + margin;
        return dx * dx + dz * dz > hardRadius * hardRadius;
    }

    /** Realm 1/2/3 share PLAYER_REALMS spawns; Realm 0 receives only explicitly REALM_0 spawns. */
    public List<MobSpawnDefinition> spawnsForRealm(int locationId, int realmId) {
        requireRealm(realmId);
        List<MobSpawnDefinition> matches = new ArrayList<>();
        for (MobSpawnDefinition spawn : catalog.spawnsForLocation(locationId)) {
            if (appliesToRealm(spawn, realmId)) matches.add(spawn);
        }
        return Collections.unmodifiableList(matches);
    }

    public boolean appliesToRealm(MobSpawnDefinition spawn, int realmId) {
        if (spawn == null) throw new IllegalArgumentException("spawn is required");
        requireRealm(realmId);
        return realmId == 0
                ? spawn.getRealmScope() == SpawnRealmScope.REALM_0
                : spawn.getRealmScope() == SpawnRealmScope.PLAYER_REALMS;
    }

    /** Uniform random X/Z point inside the Location circle. Surface Y is resolved by runtime world logic later. */
    public HorizontalPoint randomPoint(int locationId, Random random) {
        if (random == null) throw new IllegalArgumentException("random is required");
        LocationDefinition location = requireLocation(locationId);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(random.nextDouble()) * location.getRadius();
        return new HorizontalPoint(
                location.getCenterX() + Math.cos(angle) * distance,
                location.getCenterZ() + Math.sin(angle) * distance);
    }

    public ResolvedFixedSpawn resolveFixedSpawn(MobSpawnDefinition spawn, int realmId) {
        if (!appliesToRealm(spawn, realmId)) {
            throw new IllegalArgumentException(
                    "Mob Spawn " + spawn.getSpawnId() + " does not apply to Realm " + realmId);
        }
        RealmDefinition realm = requireRealm(realmId);
        return new ResolvedFixedSpawn(
                realm.getRealmId(), realm.getDimensionId(), spawn.getLocationId(), spawn.getSpawnId(), spawn.getMobId(),
                spawn.getSpawnX(), spawn.getSpawnY(), spawn.getSpawnZ(), spawn.getYaw(), spawn.getPitch());
    }

    private RealmDefinition requireRealm(int realmId) {
        RealmDefinition realm = RealmCatalog.byRealmId(realmId);
        if (realm == null) throw new IllegalArgumentException("Unknown Realm ID: " + realmId);
        return realm;
    }

    public static final class HorizontalPoint {
        private final double x;
        private final double z;

        public HorizontalPoint(double x, double z) {
            this.x = x;
            this.z = z;
        }

        public double getX() { return x; }
        public double getZ() { return z; }
    }

    public static final class ResolvedFixedSpawn {
        private final int realmId;
        private final int dimensionId;
        private final int locationId;
        private final int spawnId;
        private final int mobId;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        public ResolvedFixedSpawn(
                int realmId, int dimensionId, int locationId, int spawnId, int mobId,
                double x, double y, double z, float yaw, float pitch) {
            this.realmId = realmId;
            this.dimensionId = dimensionId;
            this.locationId = locationId;
            this.spawnId = spawnId;
            this.mobId = mobId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public int getRealmId() { return realmId; }
        public int getDimensionId() { return dimensionId; }
        public int getLocationId() { return locationId; }
        public int getSpawnId() { return spawnId; }
        public int getMobId() { return mobId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }
}
