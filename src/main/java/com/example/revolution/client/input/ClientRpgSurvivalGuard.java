package com.example.revolution.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.world.GameType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Prevents local block-damage feedback before the server rejects Survival mining. */
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
}
