package com.example.revolution.location.definition;

/** Independent entry trigger for one target Location + mobSlot stage. */
public final class StageTransitionTriggerDefinition {
    public enum DisplayMode {
        CHAT,
        ACTION_BAR
    }

    private final int worldLocationId;
    private final int mobSlotId;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final double halfHeight;
    private final DisplayMode displayMode;
    private final String lockedMessage;
    private final String enteringMessage;

    public StageTransitionTriggerDefinition(
            int worldLocationId,
            int mobSlotId,
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            double halfHeight,
            DisplayMode displayMode,
            String lockedMessage,
            String enteringMessage) {
        if (worldLocationId < 0) throw new IllegalArgumentException("worldLocationId must be non-negative");
        if (mobSlotId < 0) throw new IllegalArgumentException("mobSlotId must be non-negative");
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("trigger coordinates must be finite");
        }
        if (!Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("trigger radius must be positive and finite");
        }
        if (!Double.isFinite(halfHeight) || halfHeight <= 0.0D) {
            throw new IllegalArgumentException("trigger halfHeight must be positive and finite");
        }
        this.worldLocationId = worldLocationId;
        this.mobSlotId = mobSlotId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.halfHeight = halfHeight;
        this.displayMode = displayMode == null ? DisplayMode.ACTION_BAR : displayMode;
        this.lockedMessage = normalize(lockedMessage);
        this.enteringMessage = normalize(enteringMessage);
    }

    public int getWorldLocationId() { return worldLocationId; }
    public int getMobSlotId() { return mobSlotId; }
    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }
    public double getHalfHeight() { return halfHeight; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public String getLockedMessage() { return lockedMessage; }
    public String getEnteringMessage() { return enteringMessage; }

    public boolean contains(double x, double y, double z) {
        if (Math.abs(y - centerY) > halfHeight) return false;
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    public StageTransitionTriggerDefinition withMessages(String locked, String entering) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, mobSlotId, centerX, centerY, centerZ, radius, halfHeight,
                displayMode, locked, entering);
    }

    public StageTransitionTriggerDefinition withDisplayMode(DisplayMode mode) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, mobSlotId, centerX, centerY, centerZ, radius, halfHeight,
                mode, lockedMessage, enteringMessage);
    }

    public String key() { return worldLocationId + ":" + mobSlotId; }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
