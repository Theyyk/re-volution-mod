package com.example.revolution.client;

import com.example.revolution.network.RuneCollectionMetadataPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiButton;

/** Client-side read model for the fixed v2.3.0 collection chain. */
final class ClientRuneCollections {
    private static final int ELEMENT_BUTTON_START_ID = 100;

    private static List<String> ids = Collections.emptyList();
    private static List<String> names = Collections.emptyList();
    private static List<Integer> progress = Collections.emptyList();
    private static int unlockedMask = 1;
    private static int activeCollection;
    private static int unlockThreshold = 13;

    private ClientRuneCollections() {}

    static void reset() {
        ids = Collections.emptyList();
        names = Collections.emptyList();
        progress = Collections.emptyList();
        unlockedMask = 1;
        activeCollection = 0;
        unlockThreshold = 13;
    }

    static void accept(RuneCollectionMetadataPacket packet) {
        ids = Collections.unmodifiableList(new ArrayList<>(packet.getCollectionIds()));
        names = Collections.unmodifiableList(new ArrayList<>(packet.getDisplayNames()));
        progress = Collections.unmodifiableList(new ArrayList<>(packet.getImprovedOrHigherProgress()));
        unlockedMask = packet.getUnlockedMask();
        activeCollection = packet.getActiveCollection();
        unlockThreshold = Math.max(1, packet.getUnlockThreshold());
    }

    static List<String> ids() { return ids; }
    static List<String> names() { return names; }
    static int activeCollection() { return activeCollection; }
    static int unlockThreshold() { return unlockThreshold; }
    static boolean isUnlocked(int index) {
        return index >= 0 && index < Integer.SIZE && (unlockedMask & (1 << index)) != 0;
    }
    static int progress(int index) {
        return index >= 0 && index < progress.size() ? progress.get(index) : 0;
    }

    /**
     * Functional selector labels. The button itself remains icon-only, so this text is surfaced by
     * the existing hover tooltip without changing the Collection 1 layout contract.
     */
    static List<String> selectorNames() {
        List<String> result = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!isUnlocked(i)) {
                int source = Math.max(0, i - 1);
                result.add(name + " [закрыто: " + progress(source) + "/" + unlockThreshold + "]");
            } else if (i + 1 < names.size() && !isUnlocked(i + 1)) {
                result.add(name + " [до следующей: " + progress(i) + "/" + unlockThreshold + "]");
            } else {
                result.add(name);
            }
        }
        return result;
    }

    /** Applies the metadata read model to the existing screen without making GUI layout authoritative. */
    static boolean applyTo(RunesGuiScreen screen) {
        if (screen == null || names.isEmpty()) return false;
        screen.setCollections(selectorNames(), activeCollection);
        for (GuiButton button : screen.buttonList) {
            int index = button.id - ELEMENT_BUTTON_START_ID;
            if (index >= 0 && index < names.size()) button.enabled = isUnlocked(index);
        }
        return true;
    }
}
