package com.example.revolution.location.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure generation output. A future world adapter may turn these placements into blocks/structures. */
public final class LocationGenerationPlan {
    private final int worldLocationId;
    private final int mobSlotId;
    private final int internalLocationId;
    private final String templateId;
    private final long seed;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final int rotationDegrees;
    private final List<DecorationPlacement> decorations;

    public LocationGenerationPlan(
            int worldLocationId,
            int mobSlotId,
            int internalLocationId,
            String templateId,
            long seed,
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            int rotationDegrees,
            List<DecorationPlacement> decorations) {
        this.worldLocationId = worldLocationId;
        this.mobSlotId = mobSlotId;
        this.internalLocationId = internalLocationId;
        this.templateId = templateId;
        this.seed = seed;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.rotationDegrees = rotationDegrees;
        this.decorations = Collections.unmodifiableList(new ArrayList<>(decorations));
    }

    public int getWorldLocationId() { return worldLocationId; }
    public int getMobSlotId() { return mobSlotId; }
    public int getInternalLocationId() { return internalLocationId; }
    public String getTemplateId() { return templateId; }
    public long getSeed() { return seed; }
    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }
    public int getRotationDegrees() { return rotationDegrees; }
    public List<DecorationPlacement> getDecorations() { return decorations; }

    public static final class DecorationPlacement {
        private final LocationDecorationKind kind;
        private final double offsetX;
        private final double offsetZ;
        private final int rotationDegrees;

        public DecorationPlacement(LocationDecorationKind kind, double offsetX, double offsetZ, int rotationDegrees) {
            if (kind == null) throw new IllegalArgumentException("kind is required");
            if (!Double.isFinite(offsetX) || !Double.isFinite(offsetZ)) throw new IllegalArgumentException("offset must be finite");
            this.kind = kind;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.rotationDegrees = rotationDegrees;
        }

        public LocationDecorationKind getKind() { return kind; }
        public double getOffsetX() { return offsetX; }
        public double getOffsetZ() { return offsetZ; }
        public int getRotationDegrees() { return rotationDegrees; }
    }
}
