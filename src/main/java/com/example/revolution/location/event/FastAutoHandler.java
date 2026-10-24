package com.example.revolution.location.event;

import com.example.revolution.location.service.FastAutoService;
import com.example.revolution.location.service.StageTransitionService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge adapter that executes queued fast-auto movement on the server thread. */
public final class FastAutoHandler {
    private final FastAutoService fastAuto;
    private final StageTransitionService transitions;

    public FastAutoHandler(FastAutoService fastAuto, StageTransitionService transitions) {
        this.fastAuto = fastAuto;
        this.transitions = transitions;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Integer target = fastAuto.pollPendingTarget(player.getUniqueID());
        if (target == null) return;
        try {
            transitions.moveToStage(player, target);
        } catch (IllegalArgumentException | IllegalStateException e) {
            player.sendMessage(new TextComponentString("Fast-auto: телепорт недоступен: " + e.getMessage()));
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) fastAuto.forgetPlayer(event.player.getUniqueID());
    }
}
