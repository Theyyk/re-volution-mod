package com.example.revolution.location.definition;

/** One physical mob slot inside a Location. Multiple slots may reference the same mobId. */
public final class MobSpawnDefinition {
    private final int locationId;
    private final int spawnId;
    private final int mobId;
    private final double spawnX;
    private final double spawnY;
    private final double spawnZ;
    private final float yaw;
    private final float pitch;
    private final SpawnMode spawnMode;
    private final MovementMode movementMode;
    private final SpawnRealmScope realmScope;

    public MobSpawnDefinition(
            int locationId,
            int spawnId,
            int mobId,
            double spawnX,
            double spawnY,
            double spawnZ,
            float yaw,
            float pitch,
            SpawnMode spawnMode,
            MovementMode movementMode,
            SpawnRealmScope realmScope) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (spawnId < 0) throw new IllegalArgumentException("spawnId must be non-negative");
        if (mobId < 0) throw new IllegalArgumentException("mobId must be non-negative");
        if (!Double.isFinite(spawnX) || !Double.isFinite(spawnY) || !Double.isFinite(spawnZ)) {
            throw new IllegalArgumentException("spawn coordinates must be finite");
        }
        if (spawnMode == null) throw new IllegalArgumentException("spawnMode is required");
        if (movementMode == null) throw new IllegalArgumentException("movementMode is required");
        if (realmScope == null) throw new IllegalArgumentException("realmScope is required");
        this.locationId = locationId;
        this.spawnId = spawnId;
        this.mobId = mobId;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.spawnMode = spawnMode;
        this.movementMode = movementMode;
        this.realmScope = realmScope;
    }

    public int getLocationId() { return locationId; }
    public int getSpawnId() { return spawnId; }
    public int getMobId() { return mobId; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public SpawnMode getSpawnMode() { return spawnMode; }
    public MovementMode getMovementMode() { return movementMode; }
    public SpawnRealmScope getRealmScope() { return realmScope; }
}
