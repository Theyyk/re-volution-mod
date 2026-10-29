package com.example.revolution.mob.event;

import com.example.revolution.mob.service.ManagedMobService;
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
    public ManagedMobEvents(ManagedMobService mobs) { this.mobs = mobs; }

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
            event.setCanceled(true);
            mobs.applyDamage(event.getEntityLiving(), event.getSource(), event.getAmount());
            ManagedMobAppearanceEvents.refreshNow(event.getEntityLiving());
        }
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
