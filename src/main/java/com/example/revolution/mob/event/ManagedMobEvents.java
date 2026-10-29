package com.example.revolution.mob.event;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge subscriptions preserve priorities, thread phases and cancellation at the adapter boundary. */
public final class ManagedMobEvents {
    private final ManagedMobService mobs;
    private final GameplayEventSink gameplayEvents;

    public ManagedMobEvents(ManagedMobService mobs, GameplayEventSink gameplayEvents) {
        this.mobs = mobs;
        this.gameplayEvents = gameplayEvents == null ? GameplayEventSink.NONE : gameplayEvents;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (mobs.isStaleEntity(event.getEntity(), event.getWorld())) {
            event.setCanceled(true);
            mobs.removeStaleEntity(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChunkSave(ChunkDataEvent.Save event) {
        mobs.removePersistedMobs(event.getData(), event.getChunk().x, event.getChunk().z);
    }

    @SubscribeEvent public void onSpawnChunkUnload(ChunkEvent.Unload event) {
        mobs.chunkUnloaded(event.getWorld(), event.getChunk().x, event.getChunk().z);
    }
    @SubscribeEvent public void onSpawnChunkLoad(ChunkEvent.Load event) {
        mobs.chunkLoaded(event.getWorld(), event.getChunk().x, event.getChunk().z);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (mobs.manages(event.getEntityLiving())) {
            BigDecimal hpBefore = managedHp(event.getEntityLiving().getCustomNameTag());
            EntityPlayerMP player = event.getSource().getTrueSource() instanceof EntityPlayerMP
                    ? (EntityPlayerMP) event.getSource().getTrueSource() : null;
            GameplayDamageSource source = player != null && mobs.isApplyingPoisonDamage(player.getUniqueID())
                    ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK;

            event.setCanceled(true);
            mobs.applyDamage(event.getEntityLiving(), event.getSource(), event.getAmount());

            if (player != null && hpBefore != null) {
                BigDecimal hpAfter = event.getEntityLiving().isDead
                        ? BigDecimal.ZERO : managedHp(event.getEntityLiving().getCustomNameTag());
                if (hpAfter != null) {
                    BigDecimal dealt = hpBefore.subtract(hpAfter);
                    if (dealt.signum() > 0) gameplayEvents.confirmedDamage(player.getUniqueID(), source, dealt);
                }
            }
            ManagedMobAppearanceEvents.refreshNow(event.getEntityLiving());
        }
    }

    private static BigDecimal managedHp(String customName) {
        if (customName == null || !customName.startsWith("§c")) return null;
        String[] parts = customName.substring(2).split("\\|");
        if (parts.length < 2) return null;
        try { return new BigDecimal(parts[1]); }
        catch (NumberFormatException ignored) { return null; }
    }

    @SubscribeEvent public void onMobDeath(LivingDeathEvent event) { mobs.died(event.getEntityLiving(), event.getSource()); }
    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) mobs.tick();
    }
    @SubscribeEvent public void onMobDrops(LivingDropsEvent event) {
        if (mobs.suppressVanillaDrops(event.getEntityLiving())) { event.getDrops().clear(); event.setCanceled(true); }
    }
    @SubscribeEvent public void onMobExperience(LivingExperienceDropEvent event) {
        if (mobs.suppressVanillaDrops(event.getEntityLiving())) event.setDroppedExperience(0);
    }
}
