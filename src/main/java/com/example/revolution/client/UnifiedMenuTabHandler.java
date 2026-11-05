package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sole owner of auxiliary E-menu navigation. Uses the existing virtual canvas,
 * without replacing the v2.12 rune screen, stats, preloader or server rules.
 */
public final class UnifiedMenuTabHandler {
    private static final UiMenuSection[] TABS = UiMenuSection.auxiliaryTabs();
    private final GuiButton[] buttons = new GuiButton[TABS.length];
    private RunesGuiScreen currentScreen;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        RunesGuiScreen screen = (RunesGuiScreen) event.getGui();
        currentScreen = screen;
        GuiLayout layout = new GuiLayout(26);
        layout.update(screen.width, screen.height);
        int x = UnifiedMenuLayout.x(layout);
        int buttonSize = UnifiedMenuLayout.size(layout);
        for (int i = 0; i < TABS.length; i++) {
            UiMenuSection tab = TABS[i];
            GuiControls.PanelButton button = new GuiControls.PanelButton(
                    tab.buttonId(), x, UnifiedMenuLayout.y(layout, i), buttonSize, buttonSize, tab.shortLabel());
            applyState(button, tab);
            buttons[i] = button;
            event.getButtonList().add(button);
        }
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() != currentScreen) return;
        for (int i = 0; i < TABS.length; i++) {
            if (buttons[i] != null) applyState(buttons[i], TABS[i]);
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        UiMenuSection tab = UiMenuSection.fromButtonId(event.getButton().id);
        if (tab == null || tab == UiMenuSection.RUNES) return;
        event.setCanceled(true);
        if (!state(tab).isInteractive()) return;

        Minecraft mc = Minecraft.getMinecraft();
        switch (tab) {
            case UNIQUE_RUNES: mc.displayGuiScreen(new UniqueRunesGuiScreen()); break;
            case PETS: mc.displayGuiScreen(new PetsGuiScreen()); break;
            case MECHANIC_UNLOCKS: mc.displayGuiScreen(new MechanicUnlockGuiScreen()); break;
            case ACHIEVEMENTS: mc.displayGuiScreen(new AchievementsGuiScreen()); break;
            default: break;
        }
    }

    private static void applyState(GuiButton button, UiMenuSection tab) {
        UiAvailability state = state(tab);
        button.visible = state.isVisible();
        button.enabled = state.isInteractive();
    }

    private static UiAvailability state(UiMenuSection tab) {
        String unlockId = tab.mechanicUnlockId();
        if (unlockId == null) return UiAvailability.AVAILABLE;
        return ClientMechanicUnlocks.uiState(unlockId, UiAvailability.AVAILABLE);
    }
}
