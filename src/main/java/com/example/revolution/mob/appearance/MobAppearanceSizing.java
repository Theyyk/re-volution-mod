package com.example.revolution.mob.appearance;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;

/** Keeps the physical managed-mob bounds aligned with the custom visual family and scale. */
public final class MobAppearanceSizing {
    private MobAppearanceSizing() {}

    public static final class Dimensions {
        private final float width;
        private final float height;

        Dimensions(float width, float height) {
            this.width = width;
            this.height = height;
        }

        public float getWidth() { return width; }
        public float getHeight() { return height; }
    }

    public static Dimensions dimensions(MobAppearanceDefinition appearance) {
        if (appearance == null) return new Dimensions(0.6F, 1.8F);
        double scale = appearance.getScale();
        String family = appearance.getFamilyId();
        double width;
        double height;
        if ("turtle".equals(family)) { width = 0.90D; height = 0.60D; }
        else if ("serpent".equals(family)) { width = 0.78D; height = 0.62D; }
        else if ("mantis".equals(family)) { width = 0.72D; height = 1.55D; }
        else if ("spore".equals(family)) { width = 0.82D; height = 1.05D; }
        else if ("golem".equals(family)) { width = 0.95D; height = 1.85D; }
        else if ("spirit".equals(family)) { width = 0.72D; height = 1.45D; }
        else if ("beast".equals(family)) { width = 0.92D; height = 1.35D; }
        else if ("dryad".equals(family)) { width = 0.70D; height = 1.78D; }
        else { width = 1.25D; height = 2.25D; }
        return new Dimensions((float) (width * scale), (float) (height * scale));
    }

    public static void apply(EntityLivingBase entity, MobAppearanceDefinition appearance) {
        if (entity == null || appearance == null) return;
        Dimensions dimensions = dimensions(appearance);
        if (Math.abs(entity.width - dimensions.width) < 0.001F
                && Math.abs(entity.height - dimensions.height) < 0.001F) return;

        // Entity#setSize is protected in 1.12.2. Managed mobs deliberately keep the vanilla entity
        // implementation, so update the public dimensions and physical AABB directly around posX/posY/posZ.
        entity.width = dimensions.width;
        entity.height = dimensions.height;
        double halfWidth = dimensions.width * 0.5D;
        entity.setEntityBoundingBox(new AxisAlignedBB(
                entity.posX - halfWidth, entity.posY, entity.posZ - halfWidth,
                entity.posX + halfWidth, entity.posY + dimensions.height, entity.posZ + halfWidth));
    }
}