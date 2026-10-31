package com.example.revolution.god.event;

import com.example.revolution.god.GodWorshipAutomationService;
import com.example.revolution.network.GodWorshipRequestLimiter;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge adapter for offline/login settlement and bounded periodic God automation. */
public final class GodWorshipRuntimeHandler {
    private final GodWorshipAutomationService automation;
    private long nextTickAt;

    public GodWorshipRuntimeHandler(GodWorshipAutomationService automation) {
        if (automation == null) throw new IllegalArgumentException("God automation service is required");
        this.automation = automation;
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        automation.tick(player.getUniqueID(), System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        UUID uuid = event.player.getUniqueID();
        automation.playerDisconnected(uuid);
        GodWorshipRequestLimiter.forget(uuid);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.currentTimeMillis();
        if (now < nextTickAt) return;
        long interval = Math.max(50L, automation.processingIntervalMillis());
        nextTickAt = safeAdd(now, interval);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getPlayerList() == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUniqueID();
            if (uuid != null) automation.tick(uuid, now);
        }
    }

    private static long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
