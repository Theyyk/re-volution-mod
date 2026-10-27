package com.example.revolution.location.definition;

/** Gameplay geometry template shared by stages of one world Location. */
public final class LocationStageTemplate {
    private final int worldLocationId;
    private final double mobRadius;
    private final double triggerOffset;
    private final double triggerRadius;

    public LocationStageTemplate(int worldLocationId, double mobRadius, double triggerOffset, double triggerRadius) {
        if (worldLocationId < 0) throw new IllegalArgumentException("worldLocationId must be non-negative");
        if (!Double.isFinite(mobRadius) || mobRadius <= 0.0D) {
            throw new IllegalArgumentException("mobRadius must be positive and finite");
        }
        if (!Double.isFinite(triggerOffset) || triggerOffset < 0.0D) {
            throw new IllegalArgumentException("triggerOffset must be non-negative and finite");
        }
        if (!Double.isFinite(triggerRadius) || triggerRadius <= 0.0D) {
            throw new IllegalArgumentException("triggerRadius must be positive and finite");
        }
        this.worldLocationId = worldLocationId;
        this.mobRadius = mobRadius;
        this.triggerOffset = triggerOffset;
        this.triggerRadius = triggerRadius;
    }

    public int getWorldLocationId() { return worldLocationId; }
    public double getMobRadius() { return mobRadius; }
    public double getTriggerOffset() { return triggerOffset; }
    public double getTriggerRadius() { return triggerRadius; }
}
