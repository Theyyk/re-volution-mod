package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sole owner of E-menu auxiliary navigation actions.
 *
 * Rune GUI rebuilds its entire buttonList after asynchronous collection sync.
 * Buttons must therefore be installed by the rune screen's control rebuild,
 * rather than only by Forge InitGuiEvent.Post.
 */
public final class UnifiedMenuTabHandler {
    private static final UiMenuSection[] TABS = UiMenuSection.auxiliaryTabs();

    /** Called by RunesGuiScreen.rebuildNavigationButtons on EVERY rebuild. */
    static void installButtons(List<GuiButton> buttons, GuiLayout layout) {
        int x = UnifiedMenuLayout.x(layout);
        int buttonSize = UnifiedMenuLayout.size(layout);
        for (int i = 0; i < TABS.length; i++) {
            UiMenuSection tab = TABS[i];
            GuiControls.PanelButton button = new GuiControls.PanelButton(
                    tab.buttonId(), x, UnifiedMenuLayout.y(layout, i), buttonSize, buttonSize, tab.shortLabel());
            applyState(button, tab);
            buttons.add(button);
        }
    }

    /** Refreshes server-authoritative visibility without replacing buttons. */
    static void refreshButtons(List<GuiButton> buttons) {
        for (GuiButton button : buttons) {
            UiMenuSection tab = UiMenuSection.fromButtonId(button.id);
            if (tab != null && tab != UiMenuSection.RUNES) applyState(button, tab);
        }
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        // The rune strip has three server-gated special collection selectors.
        ClientUniqueRunes.sync();
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        UiMenuSection tab = UiMenuSection.fromButtonId(event.getButton().id);
        if (tab == null || tab == UiMenuSection.RUNES) return;
        event.setCanceled(true);
        if (!state(tab).isInteractive()) return;

        Minecraft mc = Minecraft.getMinecraft();
        UnifiedMainMenuChrome.rememberRunes((RunesGuiScreen) event.getGui());
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
