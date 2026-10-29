package com.example.revolution.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Removes vanilla Survival interactions/HUD that do not belong in the RPG client. */
@SideOnly(Side.CLIENT)
public final class ClientRpgSurvivalGuard {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.playerController == null) return;
        if (event.getEntityPlayer() != minecraft.player) return;
        if (minecraft.playerController.getCurrentGameType() != GameType.SURVIVAL) return;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.playerController == null) return;
        if (minecraft.playerController.getCurrentGameType() != GameType.SURVIVAL) return;

        switch (event.getType()) {
            case HOTBAR:
            case HEALTH:
            case FOOD:
            case EXPERIENCE:
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }
}
