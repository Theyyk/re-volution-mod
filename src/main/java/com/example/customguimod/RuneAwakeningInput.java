package com.example.customguimod;

import java.util.function.IntPredicate;

/** Resolves an awakening click from screen coordinates using the rendered layout. */
final class RuneAwakeningInput {
    private RuneAwakeningInput() {}

    static int runeAt(GuiLayout layout, RuneInventory inventory, int activeDeck,
                      int screenX, int screenY, IntPredicate matchesSearch) {
        if (activeDeck != 0 || !inventory.hasAllCatalogRunes()) return -1;
        boolean allGold = true;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry entry = inventory.get(rune);
            if (entry != null && entry.layer < RuneInventory.GOLD_RANK) allGold = false;
        }
        if (allGold) return -1;

        int x = layout.canvasMouseX(screenX);
        int y = layout.canvasMouseY(screenY);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int[] pos = layout.runeSlotPosition(rune);
            if (x < pos[0] || x >= pos[0] + layout.runeSlotWidth
                    || y < pos[1] || y >= pos[1] + layout.runeSlotHeight) continue;
            return matchesSearch.test(rune) && inventory.get(rune) != null ? rune : -1;
        }
        return -1;
    }
}
