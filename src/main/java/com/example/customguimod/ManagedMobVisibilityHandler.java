package com.example.customguimod;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.EntityTracker;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Re-runs vanilla entity visibility checks shortly after login.
 *
 * Managed mobs can already exist server-side while the reconnecting client's chunks
 * are still being attached. A plain updateVisibility call is not enough when the
 * tracker still believes the joining player already tracks an entity that the new
 * client session never received. Force-removing the player from tracker entries and
 * rebuilding visibility makes vanilla resend the existing entities without recreating
 * them or touching their managed HP/state.
 */
public class ManagedMobVisibilityHandler {
    private static final int FIRST_REFRESH_TICKS = 5;
    private static final int FINAL_REFRESH_TICKS = 40;
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
                refreshVisibility(player);
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

    private static void refreshVisibility(EntityPlayerMP player) {
        if (player == null || !(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;
        EntityTracker tracker = world.getEntityTracker();

        // A reconnect creates a fresh client world, but the server tracker can still
        // have stale per-player visibility state for entities that survived on the
        // server. Clear that state first so updateVisibility must send spawn packets
        // again for every entity the player should currently see.
        tracker.removePlayerFromTrackers(player);
        tracker.updateVisibility(player);

        CustomGuiMod.logger.debug("Rebuilt entity tracking after login for " + player.getName()
                + " at tick " + elapsedLoginTicks.getOrDefault(player.getUniqueID(), -1));
    }
}
