package com.example.revolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds Achievements alongside existing functional inventory tabs without rewriting Rune layout. */
public final class AchievementTabUiHandler {
    private static final int ACHIEVEMENT_TAB_ID = 297;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        event.getButtonList().add(new GuiButton(ACHIEVEMENT_TAB_ID, event.getGui().width - 196, 8, 80, 20, "Achievements"));
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != ACHIEVEMENT_TAB_ID) return;
        Minecraft.getMinecraft().displayGuiScreen(new AchievementsGuiScreen());
    }
}
