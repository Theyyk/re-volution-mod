package com.example.revolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds the functional Pets tab to the existing RE:volution inventory menu without rewriting Rune layout. */
public final class PetTabUiHandler {
    private static final int PET_TAB_ID = 298;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        event.getButtonList().add(new GuiButton(PET_TAB_ID, event.getGui().width - 112, 8, 76, 20, "Pets"));
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != PET_TAB_ID) return;
        Minecraft.getMinecraft().displayGuiScreen(new PetsGuiScreen());
    }
}
