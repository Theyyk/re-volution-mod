package com.example.revolution.mob.appearance;

/**
 * Immutable visual/content identity for one managed mob slot.
 *
 * <p>This definition intentionally contains no gameplay balance values such as HP, rewards or damage.
 */
public final class MobAppearanceDefinition {
    private final int locationId;
    private final int mobSlotId;
    private final String mobId;
    private final String displayName;
    private final String familyId;
    private final String appearanceId;
    private final String modelId;
    private final String textureId;
    private final String animationSetId;
    private final double scale;

    public MobAppearanceDefinition(
            int locationId,
            int mobSlotId,
            String mobId,
            String displayName,
            String familyId,
            String appearanceId,
            String modelId,
            String textureId,
            String animationSetId,
            double scale) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (mobSlotId <= 0) throw new IllegalArgumentException("mobSlotId must be positive");
        this.mobId = requireText(mobId, "mobId");
        this.displayName = requireText(displayName, "displayName");
        this.familyId = requireText(familyId, "familyId");
        this.appearanceId = requireText(appearanceId, "appearanceId");
        this.modelId = requireText(modelId, "modelId");
        this.textureId = requireText(textureId, "textureId");
        this.animationSetId = requireText(animationSetId, "animationSetId");
        if (!Double.isFinite(scale) || scale <= 0.0D) throw new IllegalArgumentException("scale must be positive and finite");
        this.locationId = locationId;
        this.mobSlotId = mobSlotId;
        this.scale = scale;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    public int getLocationId() { return locationId; }
    public int getMobSlotId() { return mobSlotId; }
    public String getMobId() { return mobId; }
    public String getDisplayName() { return displayName; }
    public String getFamilyId() { return familyId; }
    public String getAppearanceId() { return appearanceId; }
    public String getModelId() { return modelId; }
    public String getTextureId() { return textureId; }
    public String getAnimationSetId() { return animationSetId; }
    public double getScale() { return scale; }
}
