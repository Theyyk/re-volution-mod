package com.example.customguimod;

import java.util.function.IntPredicate;

/** Resolves awakening input and rune selection using the rendered layout. */
final class RuneAwakeningInput {
    private RuneAwakeningInput() {}

    static boolean isAwakeningButton(int button) {
        return button == 0;
    }

    static int runeAt(GuiLayout layout, RuneInventory inventory, int activeDeck,
                      int screenX, int screenY, IntPredicate matchesSearch) {
        int statusId = RuneStatusRegistry.statusForDeck(activeDeck);
        if (!RuneStatusRegistry.isRuneStatus(statusId) || !inventory.hasAllCatalogRunes()) return -1;

        boolean allAdvanced = true;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry entry = inventory.getBoost(rune);
            if (entry != null && entry.layer < RuneInventory.ADVANCED_RANK) {
                allAdvanced = false;
                break;
            }
        }
        if (allAdvanced) return -1;

        int x = layout.canvasMouseX(screenX);
        int y = layout.canvasMouseY(screenY);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int[] pos = layout.runeSlotPosition(rune);
            if (x < pos[0] || x >= pos[0] + layout.runeSlotWidth
                    || y < pos[1] || y >= pos[1] + layout.runeSlotHeight) continue;
            return matchesSearch.test(rune) && inventory.canAwaken(rune) ? rune : -1;
        }
        return -1;
    }
}
