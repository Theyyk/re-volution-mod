package com.example.customguimod;

import net.minecraft.entity.player.EntityPlayerMP;
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
 * are still being attached. In that timing window vanilla tracking may miss the mob
 * until it is killed and respawned. A delayed visibility refresh makes the existing
 * tracked entities visible without recreating them or touching their managed HP.
 */
public class ManagedMobVisibilityHandler {
    private static final int FIRST_REFRESH_TICKS = 5;
    private static final int FINAL_REFRESH_TICKS = 40;
    private static final Map<UUID, Integer> pendingRefreshes = new HashMap<>();

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        pendingRefreshes.put(event.player.getUniqueID(), FINAL_REFRESH_TICKS);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        pendingRefreshes.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingRefreshes.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = pendingRefreshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            EntityPlayerMP player = findPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            int remaining = entry.getValue() - 1;
            if (remaining == FIRST_REFRESH_TICKS || remaining <= 0) {
                refreshVisibility(player);
            }

            if (remaining <= 0) iterator.remove();
            else entry.setValue(remaining);
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
        world.getEntityTracker().updateVisibility(player);
        CustomGuiMod.logger.debug("Refreshed entity visibility after login for " + player.getName());
    }
}
