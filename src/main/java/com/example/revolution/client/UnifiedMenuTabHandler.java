package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.client.ui.UiAvailabilityResolver;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import java.util.Collections;
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
    private static final UiAvailabilityResolver STATES =
            new UiAvailabilityResolver(Collections.emptyMap());

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        RunesGuiScreen screen = (RunesGuiScreen) event.getGui();
        GuiLayout layout = new GuiLayout(26);
        layout.update(screen.width, screen.height);
        int x = layout.navPanelX + 2;
        int y = layout.navPanelY + 36;
        int buttonSize = layout.navPanelWidth - 4;
        for (int i = 0; i < IDS.length; i++) {
            GuiControls.PanelButton button = new GuiControls.PanelButton(
                    IDS[i], x, y + i * (buttonSize + 3), buttonSize, buttonSize, LABELS[i]);
            button.enabled = state(IDS[i]).isInteractive();
            event.getButtonList().add(button);
        }
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        for (GuiButton button : event.getGui().buttonList) {
            if (isNavigationId(button.id)) button.enabled = state(button.id).isInteractive();
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
        boolean unlocked = true;
        if (id == UNIQUE_RUNES)
            unlocked = ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS);
        if (id == ACHIEVEMENTS)
            unlocked = ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.ACHIEVEMENTS);
        return STATES.resolve("main." + id, UiAvailability.AVAILABLE, true, unlocked, true);
    }
}
