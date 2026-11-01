package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Keeps the Unique Rune section visible-but-locked until its canonical mechanic unlock. */
public final class UniqueRuneTabUiHandler {
    private static final int UNIQUE_RUNE_TAB_ID = 298;
    @SubscribeEvent public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        boolean unlocked = ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS);
        event.getButtonList().add(new GuiButton(UNIQUE_RUNE_TAB_ID, event.getGui().width - 112, 8, 104, 20, unlocked ? "Unique Runes" : "Unique [Locked]"));
    }
    @SubscribeEvent public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != UNIQUE_RUNE_TAB_ID) return;
        if (!ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)) return;
        Minecraft.getMinecraft().displayGuiScreen(new UniqueRunesGuiScreen());
    }
}
