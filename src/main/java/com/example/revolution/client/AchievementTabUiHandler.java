package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Shows Achievements as locked until the canonical mechanic unlock is permanent. */
public final class AchievementTabUiHandler {
    private static final int ACHIEVEMENT_TAB_ID = 297;
    @SubscribeEvent public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        boolean unlocked = ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.ACHIEVEMENTS);
        event.getButtonList().add(new GuiButton(ACHIEVEMENT_TAB_ID, event.getGui().width - 196, 8, 80, 20, unlocked ? "Achievements" : "Achievements [Locked]"));
    }
    @SubscribeEvent public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != ACHIEVEMENT_TAB_ID) return;
        if (!ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.ACHIEVEMENTS)) return;
        Minecraft.getMinecraft().displayGuiScreen(new AchievementsGuiScreen());
    }
}
