package com.example.revolution.location.definition;

/** Persisted configuration for one physical mob spawn slot. */
public final class StoredMobSpawn {
    private final int locationId;
    private final int spawnId;
    private final String mobName;
    private final double spawnX;
    private final double spawnY;
    private final double spawnZ;
    private final float yaw;
    private final float pitch;
    private final SpawnMode spawnMode;
    private final MovementMode movementMode;
    private final SpawnRealmScope realmScope;
    private final boolean enabled;

    public StoredMobSpawn(
            int locationId,
            int spawnId,
            String mobName,
            double spawnX,
            double spawnY,
            double spawnZ,
            float yaw,
            float pitch,
            SpawnMode spawnMode,
            MovementMode movementMode,
            SpawnRealmScope realmScope,
            boolean enabled) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (spawnId < 0) throw new IllegalArgumentException("spawnId must be non-negative");
        if (mobName == null || mobName.trim().isEmpty()) throw new IllegalArgumentException("mobName is required");
        if (!Double.isFinite(spawnX) || !Double.isFinite(spawnY) || !Double.isFinite(spawnZ)) {
            throw new IllegalArgumentException("spawn coordinates must be finite");
        }
        if (spawnMode == null) throw new IllegalArgumentException("spawnMode is required");
        if (movementMode == null) throw new IllegalArgumentException("movementMode is required");
        if (realmScope == null) throw new IllegalArgumentException("realmScope is required");
        this.locationId = locationId;
        this.spawnId = spawnId;
        this.mobName = mobName.trim();
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.spawnMode = spawnMode;
        this.movementMode = movementMode;
        this.realmScope = realmScope;
        this.enabled = enabled;
    }

    public int getLocationId() { return locationId; }
    public int getSpawnId() { return spawnId; }
    public String getMobName() { return mobName; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public SpawnMode getSpawnMode() { return spawnMode; }
    public MovementMode getMovementMode() { return movementMode; }
    public SpawnRealmScope getRealmScope() { return realmScope; }
    public boolean isEnabled() { return enabled; }

    public MobSpawnDefinition toDefinition(int mobId) {
        return new MobSpawnDefinition(locationId, spawnId, mobId, spawnX, spawnY, spawnZ, yaw, pitch,
                spawnMode, movementMode, realmScope);
    }
}
