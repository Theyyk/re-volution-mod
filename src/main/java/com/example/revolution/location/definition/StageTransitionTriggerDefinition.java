package com.example.revolution.location.definition;

/** One annular trigger around a source mob stage, split into forward/backward 180-degree halves. */
public final class StageTransitionTriggerDefinition {
    public enum DisplayMode {
        CHAT,
        ACTION_BAR
    }

    private final int worldLocationId;
    private final int sourceMobSlotId;
    private final double offset;
    private final double radius;
    private final DisplayMode displayMode;
    private final String lockedMessage;
    private final String forwardMessage;
    private final String backwardMessage;

    public StageTransitionTriggerDefinition(
            int worldLocationId,
            int sourceMobSlotId,
            double offset,
            double radius,
            DisplayMode displayMode,
            String lockedMessage,
            String forwardMessage,
            String backwardMessage) {
        if (worldLocationId < 0) throw new IllegalArgumentException("worldLocationId must be non-negative");
        if (sourceMobSlotId < 1 || sourceMobSlotId >= 100) {
            throw new IllegalArgumentException("sourceMobSlotId must be between 1 and 99");
        }
        if (!Double.isFinite(offset) || offset < 0.0D) {
            throw new IllegalArgumentException("trigger offset must be non-negative and finite");
        }
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("trigger radius must be positive and finite");
        }
        this.worldLocationId = worldLocationId;
        this.sourceMobSlotId = sourceMobSlotId;
        this.offset = offset;
        this.radius = radius;
        this.displayMode = displayMode == null ? DisplayMode.ACTION_BAR : displayMode;
        this.lockedMessage = normalize(lockedMessage);
        this.forwardMessage = normalize(forwardMessage);
        this.backwardMessage = normalize(backwardMessage);
    }

    public int getWorldLocationId() { return worldLocationId; }
    public int getSourceMobSlotId() { return sourceMobSlotId; }
    public double getOffset() { return offset; }
    public double getRadius() { return radius; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public String getLockedMessage() { return lockedMessage; }
    public String getForwardMessage() { return forwardMessage; }
    public String getBackwardMessage() { return backwardMessage; }

    /** Trigger starts offset blocks outside the mob-zone and extends radius blocks farther out. */
    public boolean containsBoundaryRing(LocationDefinition source, double x, double z) {
        if (source == null) return false;
        double dx = x - source.getCenterX();
        double dz = z - source.getCenterZ();
        double distanceSquared = dx * dx + dz * dz;
        double inner = source.getRadius() + offset;
        double outer = inner + radius;
        return distanceSquared >= inner * inner && distanceSquared <= outer * outer;
    }

    /** Splits the annulus into exact 180-degree forward/backward halves. */
    public boolean isForwardHalf(LocationDefinition source, double x, double z, double forwardX, double forwardZ) {
        if (source == null) return false;
        double axisLengthSquared = forwardX * forwardX + forwardZ * forwardZ;
        if (axisLengthSquared <= 1.0E-12D) return false;
        double playerX = x - source.getCenterX();
        double playerZ = z - source.getCenterZ();
        return playerX * forwardX + playerZ * forwardZ >= 0.0D;
    }

    public StageTransitionTriggerDefinition withMessages(String locked, String forward, String backward) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, sourceMobSlotId, offset, radius,
                displayMode, locked, forward, backward);
    }

    public StageTransitionTriggerDefinition withDisplayMode(DisplayMode mode) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, sourceMobSlotId, offset, radius, mode,
                lockedMessage, forwardMessage, backwardMessage);
    }

    public String key() { return worldLocationId + ":" + sourceMobSlotId; }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
