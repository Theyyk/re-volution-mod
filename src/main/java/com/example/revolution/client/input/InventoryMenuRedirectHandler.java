package com.example.revolution.client.input;

import com.example.revolution.client.ClientUiBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Replaces the vanilla player inventory screen with the RE:volution RPG menu. */
@SideOnly(Side.CLIENT)
public final class InventoryMenuRedirectHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiInventory)) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        event.setCanceled(true);
        if (!ClientUiBridge.isRuneOpenPending()) {
            ClientUiBridge.beginRuneOpen();
        }
    }
}
