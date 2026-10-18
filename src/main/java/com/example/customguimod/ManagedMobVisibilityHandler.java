package com.example.customguimod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketSpawnMob;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Repairs managed-mob visibility for reconnecting clients.
 *
 * Vanilla tracking can retain enough server-side state that updateVisibility() alone
 * does not resend an already-existing managed mob to a fresh client world. Repeatedly
 * removing the player from all tracker entries also proved unreliable across several
 * reconnects. Keep vanilla tracking intact and explicitly resend only current-session
 * managed mobs after the player's chunks have had time to attach.
 */
public class ManagedMobVisibilityHandler {
    private static final int FIRST_REFRESH_TICKS = 10;
    private static final int SECOND_REFRESH_TICKS = 40;
    private static final int FINAL_REFRESH_TICKS = 100;
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
            if (elapsed == FIRST_REFRESH_TICKS || elapsed == SECOND_REFRESH_TICKS || elapsed == FINAL_REFRESH_TICKS) {
                refreshVisibility(player, elapsed);
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

    private static void refreshVisibility(EntityPlayerMP player, int elapsedTicks) {
        if (player == null || player.connection == null || !(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;
        EntityTracker tracker = world.getEntityTracker();

        // Let vanilla refresh its normal tracker state, but do not remove the player
        // from every tracker entry. Explicitly rebuild only managed mobs on the client.
        tracker.updateVisibility(player);

        int resent = 0;
        for (Entity raw : world.loadedEntityList) {
            if (!(raw instanceof EntityLiving) || raw.isDead) continue;
            if (raw.getEntityData().getString(SESSION_TAG).isEmpty()) continue;

            EntityLiving entity = (EntityLiving) raw;
            player.connection.sendPacket(new SPacketDestroyEntities(entity.getEntityId()));
            player.connection.sendPacket(new SPacketSpawnMob(entity));
            player.connection.sendPacket(new SPacketEntityMetadata(entity.getEntityId(), entity.getDataManager(), true));
            resent++;
        }

        CustomGuiMod.logger.debug("Refreshed managed mob visibility for " + player.getName()
                + " at tick " + elapsedTicks + "; resent=" + resent);
    }
}
