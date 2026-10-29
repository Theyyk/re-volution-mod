package com.example.revolution.combat;

import com.example.revolution.mob.service.ManagedMobService;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Extends the managed-mob combat pause to server-authoritative poison ticks. */
public final class PoisonMobPauseHandler {
    private static final String PAUSE_TAG = "revolutionPoisonCombatPause";
    private static final int COMBAT_PAUSE_TICKS = 20;
    private final ManagedMobService mobs;

    public PoisonMobPauseHandler(ManagedMobService mobs) {
        if (mobs == null) throw new IllegalArgumentException("managed mob service is required");
        this.mobs = mobs;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        if (!mobs.isApplyingPoisonDamage(player.getUniqueID())) return;
        if (!mobs.suppressVanillaDrops(entity)) return;

        entity.getEntityData().setInteger(PAUSE_TAG, COMBAT_PAUSE_TICKS);
        stop(entity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;
        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        if (entity.world.isRemote || !mobs.suppressVanillaDrops(entity)) return;

        NBTTagCompound data = entity.getEntityData();
        int ticks = data.getInteger(PAUSE_TAG);
        if (ticks <= 0) return;

        stop(entity);
        if (ticks <= 1) data.removeTag(PAUSE_TAG);
        else data.setInteger(PAUSE_TAG, ticks - 1);
    }

    private static void stop(EntityLiving entity) {
        entity.getNavigator().clearPath();
        entity.motionX = 0.0D;
        entity.motionZ = 0.0D;
        entity.velocityChanged = true;
    }
}
