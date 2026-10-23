package com.example.revolution.location.event;

import com.example.revolution.location.service.StageTransitionTriggerService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Server tick adapter for configurable stage transition trigger zones. */
public final class StageTransitionTriggerHandler {
    private final StageTransitionTriggerService triggers;

    public StageTransitionTriggerHandler(StageTransitionTriggerService triggers) {
        this.triggers = triggers;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        triggers.handlePlayerTick((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) triggers.forgetPlayer(event.player.getUniqueID());
    }
}
