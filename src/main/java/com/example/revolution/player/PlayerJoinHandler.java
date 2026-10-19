package com.example.revolution.player;

import com.example.revolution.player.service.PlayerSessionService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Login adapter; collection initialization belongs to the session service. */
public final class PlayerJoinHandler {
    private final PlayerSessionService sessions;
    public PlayerJoinHandler(PlayerSessionService sessions) { this.sessions = sessions; }
    @SubscribeEvent public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) sessions.joined((EntityPlayerMP) event.player);
    }
}
