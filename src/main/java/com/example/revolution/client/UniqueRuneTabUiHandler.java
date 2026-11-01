package com.example.revolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds the dedicated Unique Rune section alongside the existing Rune screen. */
public final class UniqueRuneTabUiHandler {
    private static final int UNIQUE_RUNE_TAB_ID = 298;

    @SubscribeEvent public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        event.getButtonList().add(new GuiButton(UNIQUE_RUNE_TAB_ID, event.getGui().width - 112, 8, 104, 20, "Unique Runes"));
    }
    @SubscribeEvent public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != UNIQUE_RUNE_TAB_ID) return;
        Minecraft.getMinecraft().displayGuiScreen(new UniqueRunesGuiScreen());
    }
}
