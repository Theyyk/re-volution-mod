package com.example.customguimod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repairs managed-mob visibility for reconnecting clients.
 *
 * Runtime testing showed that both updateVisibility() and direct spawn-packet resends
 * can still leave old managed entities invisible after repeated reconnects. The
 * important difference is that a mob killed and freshly respawned becomes reliable,
 * which points at a stale EntityTrackerEntry rather than at the entity itself.
 *
 * Rebuild the tracker entries for current-session managed mobs after login. The
 * entities themselves are not killed/recreated, so managed HP/state stays intact.
 */
public class ManagedMobVisibilityHandler {
    private static final int FIRST_REFRESH_TICKS = 20;
    private static final int FINAL_REFRESH_TICKS = 80;
    private static final String SESSION_TAG = "revolutionManagedSession";
    private static final Map<UUID, Integer> elapsedLoginTicks = new HashMap<>();

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        elapsedLoginTicks.put(event.player.getUniqueID(), 0);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        elapsedLoginTicks.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || elapsedLoginTicks.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = elapsedLoginTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            EntityPlayerMP player = findPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            int elapsed = entry.getValue() + 1;
            if (elapsed == FIRST_REFRESH_TICKS || elapsed == FINAL_REFRESH_TICKS) {
                rebuildManagedTracking(player, elapsed);
            }

            if (elapsed >= FINAL_REFRESH_TICKS) iterator.remove();
            else entry.setValue(elapsed);
        }
    }

    private static EntityPlayerMP findPlayer(UUID uuid) {
        if (uuid == null) return null;
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return null;
        return server.getPlayerList().getPlayerByUUID(uuid);
    }

    private static void rebuildManagedTracking(EntityPlayerMP player, int elapsedTicks) {
        if (player == null || player.connection == null || !(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;
        EntityTracker tracker = world.getEntityTracker();

        // Snapshot first because untrack/track mutates tracker internals. We only touch
        // live mobs created by the current managed-mob session; vanilla and stale legacy
        // entities remain outside this recovery path.
        List<EntityLiving> managed = new ArrayList<>();
        for (Entity raw : new ArrayList<>(world.loadedEntityList)) {
            if (!(raw instanceof EntityLiving) || raw.isDead) continue;
            if (raw.getEntityData().getString(SESSION_TAG).isEmpty()) continue;
            managed.add((EntityLiving) raw);
        }

        int rebuilt = 0;
        for (EntityLiving entity : managed) {
            if (entity.isDead || entity.world != world) continue;
            try {
                tracker.untrack(entity);
                tracker.track(entity);
                rebuilt++;
            } catch (RuntimeException ex) {
                CustomGuiMod.logger.warn("Could not rebuild tracking for managed mob "
                        + entity.getUniqueID() + ": " + ex.getMessage());
            }
        }

        // Re-evaluate visibility for the reconnecting player after all entries exist.
        tracker.updateVisibility(player);
        CustomGuiMod.logger.debug("Rebuilt managed mob tracker entries for " + player.getName()
                + " at tick " + elapsedTicks + "; rebuilt=" + rebuilt);
    }
}
