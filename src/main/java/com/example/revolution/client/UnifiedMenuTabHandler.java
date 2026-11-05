package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * One owner for E-menu navigation. Does not alter the rune canvas or game logic.
 * UI-02 will embed these destinations as tabs; for now it preserves their screens.
 */
public final class UnifiedMenuTabHandler {
    private static final int UNIQUE_RUNES = 300;
    private static final int PETS = 301;
    private static final int UNLOCKS = 302;
    private static final int ACHIEVEMENTS = 303;
    private static final int[] IDS = {UNIQUE_RUNES, PETS, UNLOCKS, ACHIEVEMENTS};
    private static final String[] LABELS = {"У", "П", "Р", "Д"};
    private final GuiButton[] buttons = new GuiButton[IDS.length];
    private RunesGuiScreen currentScreen;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        RunesGuiScreen screen = (RunesGuiScreen) event.getGui();
        currentScreen = screen;
        GuiLayout layout = new GuiLayout(26);
        layout.update(screen.width, screen.height);
        int x = layout.navPanelX + 2;
        int y = layout.navPanelY + 36;
        int buttonSize = layout.navPanelWidth - 4;
        for (int i = 0; i < IDS.length; i++) {
            GuiControls.PanelButton button = new GuiControls.PanelButton(
                    IDS[i], x, y + i * (buttonSize + 3), buttonSize, buttonSize, LABELS[i]);
            UiAvailability state = state(IDS[i]);
            button.visible = state.isVisible();
            button.enabled = state.isInteractive();
            buttons[i] = button;
            event.getButtonList().add(button);
        }
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() != currentScreen) return;
        for (GuiButton button : buttons) {
            if (button != null) {
                UiAvailability state = state(button.id);
                button.visible = state.isVisible();
                button.enabled = state.isInteractive();
            }
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || !isNavigationId(event.getButton().id)) return;
        event.setCanceled(true);
        int id = event.getButton().id;
        if (!state(id).isInteractive()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (id == UNIQUE_RUNES) mc.displayGuiScreen(new UniqueRunesGuiScreen());
        else if (id == PETS) mc.displayGuiScreen(new PetsGuiScreen());
        else if (id == UNLOCKS) mc.displayGuiScreen(new MechanicUnlockGuiScreen());
        else if (id == ACHIEVEMENTS) mc.displayGuiScreen(new AchievementsGuiScreen());
    }

    private static boolean isNavigationId(int id) {
        for (int value : IDS) if (value == id) return true;
        return false;
    }

    private static UiAvailability state(int id) {
        if (id == UNIQUE_RUNES)
            return ClientMechanicUnlocks.uiState(BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS, UiAvailability.AVAILABLE);
        if (id == ACHIEVEMENTS)
            return ClientMechanicUnlocks.uiState(BalanceMechanicUnlockPolicy.ACHIEVEMENTS, UiAvailability.AVAILABLE);
        // Pets and the Unlock Journal are currently not gated by v2.12 unlock IDs.
        return UiAvailability.AVAILABLE;
    }
}
