package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.util.List;
import net.minecraft.client.gui.GuiButton;

/**
 * Three special Rune collection selectors at the END of the ordinary collection strip.
 * Not part of the E-menu sidebar or the Mechanic Unlock Journal.
 * The server snapshots are authoritative for both visibility and interaction.
 */
final class UniqueRuneInlineTabs {
    static final int FIRST_ID = 140;
    static final int COUNT = 3;
    private static final int WIDTH = 46;
    private static final int GAP = 4;
    private static final String[] IDS = {
            "CRYSTAL_COLLECTION", "DAILY_COLLECTION", "OFFERINGS_COLLECTION"
    };
    private static final String[] LABELS = {"Крист.", "Ежедн.", "Поднош."};

    private UniqueRuneInlineTabs() {}

    static String collectionId(int index) { return IDS[index]; }
    static String label(int index) { return LABELS[index]; }
    static int buttonId(int index) { return FIRST_ID + index; }
    static boolean isButton(int id) { return id >= FIRST_ID && id < FIRST_ID + COUNT; }
    static int index(int id) { return id - FIRST_ID; }

    static int totalWidth() { return COUNT * WIDTH + (COUNT - 1) * GAP; }

    static int positionX(GuiLayout layout, int lastOrdinaryRight) {
        int panelRight = layout.runePanelX + layout.runePanelWidth - 8;
        // Reserve normal rune collections without moving or resizing their buttons.
        return Math.max(lastOrdinaryRight + 10, panelRight - totalWidth());
    }

    static void install(List<GuiButton> buttons, GuiLayout layout, int lastOrdinaryRight) {
        int x = positionX(layout, lastOrdinaryRight);
        int right = layout.runePanelX + layout.runePanelWidth - 8;
        if (x + totalWidth() > right) return; // Never overlap normal collection selectors.
        int y = layout.runePanelY + 57;
        for (int i = 0; i < COUNT; i++) {
            GuiButton button = new GuiControls.PanelButton(
                    buttonId(i), x + i * (WIDTH + GAP), y, WIDTH, 18, label(i));
            apply(button, i);
            buttons.add(button);
        }
    }

    static void refresh(List<GuiButton> buttons) {
        for (GuiButton button : buttons) {
            if (isButton(button.id)) apply(button, index(button.id));
        }
    }

    private static void apply(GuiButton button, int index) {
        UiAvailability state = state(index);
        button.visible = state.isVisible();
        button.enabled = state.isInteractive();
    }

    static UiAvailability state(int index) {
        UiAvailability parent = ClientMechanicUnlocks.uiState(
                BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS, UiAvailability.AVAILABLE);
        if (parent != UiAvailability.AVAILABLE) return parent;
        for (UniqueRuneSnapshotPacket.Collection collection : ClientUniqueRunes.snapshot().getCollections()) {
            if (collectionId(index).equals(collection.collectionId))
                return collection.unlocked ? UiAvailability.AVAILABLE : UiAvailability.VISIBLE_LOCKED;
        }
        // Unknown until a server snapshot arrives; never assume an unlock.
        return UiAvailability.VISIBLE_LOCKED;
    }
}
