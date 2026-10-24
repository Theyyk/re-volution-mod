package com.example.revolution.location.definition;

/** Boundary ring trigger attached to one source mob stage. */
public final class StageTransitionTriggerDefinition {
    public enum DisplayMode {
        CHAT,
        ACTION_BAR
    }

    public enum Direction {
        NEXT,
        PREVIOUS
    }

    private final int worldLocationId;
    private final int sourceMobSlotId;
    private final Direction direction;
    private final double width;
    private final DisplayMode displayMode;
    private final String lockedMessage;
    private final String enteringMessage;

    public StageTransitionTriggerDefinition(
            int worldLocationId,
            int sourceMobSlotId,
            Direction direction,
            double width,
            DisplayMode displayMode,
            String lockedMessage,
            String enteringMessage) {
        if (worldLocationId < 0) throw new IllegalArgumentException("worldLocationId must be non-negative");
        if (sourceMobSlotId < 1 || sourceMobSlotId >= 100) {
            throw new IllegalArgumentException("sourceMobSlotId must be between 1 and 99");
        }
        if (direction == null) throw new IllegalArgumentException("direction is required");
        if (!Double.isFinite(width) || width <= 0.0D) {
            throw new IllegalArgumentException("trigger width must be positive and finite");
        }
        this.worldLocationId = worldLocationId;
        this.sourceMobSlotId = sourceMobSlotId;
        this.direction = direction;
        this.width = width;
        this.displayMode = displayMode == null ? DisplayMode.ACTION_BAR : displayMode;
        this.lockedMessage = normalize(lockedMessage);
        this.enteringMessage = normalize(enteringMessage);
    }

    public int getWorldLocationId() { return worldLocationId; }
    public int getSourceMobSlotId() { return sourceMobSlotId; }
    public Direction getDirection() { return direction; }
    public double getWidth() { return width; }
    public DisplayMode getDisplayMode() { return displayMode; }
    public String getLockedMessage() { return lockedMessage; }
    public String getEnteringMessage() { return enteringMessage; }

    /** True only in the outer band, never inside the source mob-zone itself. */
    public boolean containsBoundaryRing(LocationDefinition source, double x, double z) {
        if (source == null) return false;
        double dx = x - source.getCenterX();
        double dz = z - source.getCenterZ();
        double distanceSquared = dx * dx + dz * dz;
        double inner = source.getRadius();
        double outer = inner + width;
        return distanceSquared > inner * inner && distanceSquared <= outer * outer;
    }

    public StageTransitionTriggerDefinition withMessages(String locked, String entering) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, sourceMobSlotId, direction, width,
                displayMode, locked, entering);
    }

    public StageTransitionTriggerDefinition withDisplayMode(DisplayMode mode) {
        return new StageTransitionTriggerDefinition(
                worldLocationId, sourceMobSlotId, direction, width,
                mode, lockedMessage, enteringMessage);
    }

    public String key() {
        return worldLocationId + ":" + sourceMobSlotId + ":" + direction.name();
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
