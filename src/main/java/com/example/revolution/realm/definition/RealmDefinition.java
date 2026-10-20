package com.example.revolution.realm.definition;

/** Stable Realm identity. Realm IDs are gameplay IDs and are intentionally distinct from Forge dimension IDs. */
public final class RealmDefinition {
    private final int realmId;
    private final int dimensionId;
    private final String name;
    private final int startX;
    private final int startY;
    private final int startZ;

    public RealmDefinition(int realmId, int dimensionId, String name, int startX, int startY, int startZ) {
        if (realmId < 0) throw new IllegalArgumentException("realmId must be non-negative");
        if (realmId == 0 && dimensionId != 0) throw new IllegalArgumentException("Realm 0 must use overworld dimension 0");
        if (realmId != 0 && dimensionId == 0) throw new IllegalArgumentException("dimensionId 0 is reserved for Realm 0");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        this.realmId = realmId;
        this.dimensionId = dimensionId;
        this.name = name;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
    }

    public int getRealmId() { return realmId; }
    public int getDimensionId() { return dimensionId; }
    public String getName() { return name; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getStartZ() { return startZ; }
}
