package com.example.revolution.location.service;

/** Deterministic population layout for ordinary mob zones. */
public final class MobZoneLayout {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final double FIXED_RADIUS_FACTOR = 0.85D;

    private MobZoneLayout() {}

    /** Stable 50/50 split: for odd counts the extra mob is FIXED_POINT. */
    public static int fixedCount(int totalCount) {
        if (totalCount <= 0) throw new IllegalArgumentException("count must be positive");
        return (totalCount + 1) / 2;
    }

    public static int randomCount(int totalCount) {
        return totalCount - fixedCount(totalCount);
    }

    /** Golden-angle disk layout spreads fixed points across the whole circle instead of only its edge. */
    public static HorizontalPoint fixedPoint(int index, int fixedCount, double centerX, double centerZ, double radius) {
        if (fixedCount <= 0) throw new IllegalArgumentException("fixedCount must be positive");
        if (index < 0 || index >= fixedCount) throw new IllegalArgumentException("index outside fixed population");
        if (!Double.isFinite(radius) || radius <= 0.0D) throw new IllegalArgumentException("radius must be positive");

        double normalizedRadius = Math.sqrt((index + 0.5D) / fixedCount);
        double distance = radius * FIXED_RADIUS_FACTOR * normalizedRadius;
        double angle = index * GOLDEN_ANGLE;
        return new HorizontalPoint(centerX + Math.cos(angle) * distance, centerZ + Math.sin(angle) * distance);
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
}
