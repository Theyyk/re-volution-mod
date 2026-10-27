package com.example.revolution.mob.appearance;

/** Compact client-visible marker appended to the existing managed-mob custom-name wire payload. */
public final class MobVisualWire {
    private static final String PREFIX = "rvvis:";

    private MobVisualWire() {}

    public static String encode(int locationId, int mobSlotId) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (mobSlotId <= 0) throw new IllegalArgumentException("mobSlotId must be positive");
        return PREFIX + locationId + ":" + mobSlotId;
    }

    public static int[] decode(String token) {
        if (token == null || !token.startsWith(PREFIX)) return null;
        String[] parts = token.substring(PREFIX.length()).split(":", -1);
        if (parts.length != 2) return null;
        try {
            int locationId = Integer.parseInt(parts[0]);
            int mobSlotId = Integer.parseInt(parts[1]);
            if (locationId < 0 || mobSlotId <= 0) return null;
            return new int[] {locationId, mobSlotId};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static MobAppearanceDefinition resolve(String token) {
        int[] key = decode(token);
        return key == null ? null : MobAppearanceCatalog.bySlot(key[0], key[1]);
    }
}
