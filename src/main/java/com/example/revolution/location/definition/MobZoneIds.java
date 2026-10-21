package com.example.revolution.location.definition;

/** Stable internal ID codec for a mob zone addressed by world Location ID + mob slot ID. */
public final class MobZoneIds {
    private static final int PREFIX = 1_000_000_000;
    private static final int SLOT_FACTOR = 100;
    private static final int MAX_WORLD_LOCATION_ID = 10_000_000;

    private MobZoneIds() {}

    public static int encode(int worldLocationId, int mobSlotId) {
        if (worldLocationId < 0 || worldLocationId > MAX_WORLD_LOCATION_ID) {
            throw new IllegalArgumentException("locationId must be between 0 and " + MAX_WORLD_LOCATION_ID);
        }
        if (mobSlotId < 1 || mobSlotId >= SLOT_FACTOR) {
            throw new IllegalArgumentException("mobSlotId must be between 1 and 99");
        }
        return PREFIX + worldLocationId * SLOT_FACTOR + mobSlotId;
    }

    public static boolean isMobZoneId(int internalLocationId) {
        if (internalLocationId < PREFIX) return false;
        int remainder = internalLocationId - PREFIX;
        int slot = remainder % SLOT_FACTOR;
        int worldLocationId = remainder / SLOT_FACTOR;
        return worldLocationId >= 0 && worldLocationId <= MAX_WORLD_LOCATION_ID && slot >= 1 && slot < SLOT_FACTOR;
    }

    public static int worldLocationId(int internalLocationId) {
        requireZone(internalLocationId);
        return (internalLocationId - PREFIX) / SLOT_FACTOR;
    }

    public static int mobSlotId(int internalLocationId) {
        requireZone(internalLocationId);
        return (internalLocationId - PREFIX) % SLOT_FACTOR;
    }

    private static void requireZone(int internalLocationId) {
        if (!isMobZoneId(internalLocationId)) throw new IllegalArgumentException("not a mob-zone location ID");
    }
}
