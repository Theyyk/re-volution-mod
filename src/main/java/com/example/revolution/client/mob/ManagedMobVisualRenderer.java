package com.example.revolution.client.mob;

import com.example.revolution.client.hud.HealthBarRenderer;
import com.example.revolution.mob.appearance.MobAnimationState;
import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import com.example.revolution.mob.appearance.MobVisualWire;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Selectively replaces vanilla zombie/skeleton visuals only for managed Location 1 entities carrying rvvis markers. */
@SideOnly(Side.CLIENT)
public final class ManagedMobVisualRenderer {
    private static final long HURT_VISUAL_TICKS = 4L;
    private static final long SPAWN_VISUAL_TICKS = 12L;
    private static final long DEATH_VISUAL_TICKS = 14L;

    private final ForestMobTextureLibrary textures = new ForestMobTextureLibrary();
    private final Map<String, ForestMobModel> models = new HashMap<>();
    private final Map<Integer, String> lastHp = new HashMap<>();
    private final Map<Integer, Long> hurtUntil = new HashMap<>();
    private final Map<Integer, Long> deathStarted = new HashMap<>();

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return;
        MobAppearanceDefinition appearance = appearance(entity);
        if (appearance == null) return;

        event.setCanceled(true);
        float partial = event.getPartialRenderTick();
        MobAnimationState state = animationState(entity, partial);
        ForestMobModel model = models.get(appearance.getAppearanceId());
        if (model == null) {
            model = new ForestMobModel(appearance);
            models.put(appearance.getAppearanceId(), model);
        }
        model.animate(state, entity, partial);

        float bodyYaw = interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partial);
        float headYaw = interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partial) - bodyYaw;
        float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partial;
        float limbAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partial;
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partial);
        float age = entity.ticksExisted + partial;

        GlStateManager.pushMatrix();
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);

        float scale = (float) appearance.getScale();
        if (state == MobAnimationState.SPAWN) {
            float progress = Math.min(1.0F, (entity.ticksExisted + partial) / (float) SPAWN_VISUAL_TICKS);
            scale *= 0.25F + 0.75F * easeOut(progress);
            GlStateManager.translate(0.0F, (1.0F - progress) * 0.35F, 0.0F);
        } else if (state == MobAnimationState.HURT) {
            GlStateManager.rotate((float) Math.sin(age * 2.5F) * 5.0F, 0.0F, 0.0F, 1.0F);
        } else if (state == MobAnimationState.DEATH) {
            long now = worldTick(entity);
            Long start = deathStarted.get(entity.getEntityId());
            float progress = start == null ? 0.0F : Math.min(1.0F, (now - start + partial) / (float) DEATH_VISUAL_TICKS);
            GlStateManager.rotate(90.0F * smooth(progress), 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, progress * 0.22F, 0.0F);
        }

        GlStateManager.scale(scale, scale, scale);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.translate(0.0F, -1.5F, 0.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(textures.textureFor(appearance));
        model.render(entity, limbSwing, limbAmount, age, headYaw, headPitch, 0.0625F);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        HealthBarRenderer.renderManagedBar(entity, event.getX(), event.getY(), event.getZ());
    }

    private MobAppearanceDefinition appearance(EntityLivingBase entity) {
        if (!entity.hasCustomName()) return null;
        String raw = entity.getCustomNameTag();
        if (raw == null || !raw.startsWith("§c") || !raw.contains("|")) return null;
        String[] parts = raw.substring(2).split("\\|", -1);
        if (parts.length < 6) return null;
        return MobVisualWire.resolve(parts[5]);
    }

    private MobAnimationState animationState(EntityLivingBase entity, float partial) {
        String[] parts = entity.getCustomNameTag().substring(2).split("\\|", -1);
        String hp = parts.length > 1 ? parts[1] : "";
        int id = entity.getEntityId();
        long now = worldTick(entity);
        String previous = lastHp.put(id, hp);

        if (isZero(hp)) {
            if (!deathStarted.containsKey(id)) deathStarted.put(id, now);
            return MobAnimationState.DEATH;
        }
        deathStarted.remove(id);

        if (previous != null && !previous.equals(hp)) hurtUntil.put(id, now + HURT_VISUAL_TICKS);
        Long hurt = hurtUntil.get(id);
        if (hurt != null && now <= hurt) return MobAnimationState.HURT;
        if (entity.ticksExisted < SPAWN_VISUAL_TICKS) return MobAnimationState.SPAWN;
        double horizontalMotion = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        if (horizontalMotion > 0.0004D || entity.limbSwingAmount > 0.04F) return MobAnimationState.WALK;
        return MobAnimationState.IDLE;
    }

    private static boolean isZero(String hp) {
        try { return new BigDecimal(hp).signum() <= 0; }
        catch (RuntimeException ignored) { return false; }
    }

    private static long worldTick(EntityLivingBase entity) {
        return entity.world == null ? entity.ticksExisted : entity.world.getTotalWorldTime();
    }

    private static float interpolateRotation(float previous, float current, float partial) {
        float delta = current - previous;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return previous + partial * delta;
    }

    private static float easeOut(float value) {
        float inv = 1.0F - value;
        return 1.0F - inv * inv;
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
