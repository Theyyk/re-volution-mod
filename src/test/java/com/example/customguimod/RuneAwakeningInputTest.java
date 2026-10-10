package com.example.customguimod;

/** Exercises the actual selector used by the Forge middle-button event handler. */
public final class RuneAwakeningInputTest {
    static void run() {
        RuneInventory inventory = new RuneInventory();
        GuiLayout layout = new GuiLayout(26);
        int[][] viewports = {{1280, 669}, {1920, 1080}, {2560, 1369}, {2560, 1080}, {640, 360}};
        fill(inventory, RuneInventory.NORMAL_RANK);
        for (int[] viewport : viewports) {
            layout.update(viewport[0], viewport[1]);
            for (int rune = 0; rune < 26; rune++) {
                int[] pos = layout.runeSlotPosition(rune);
                int x = screenX(layout, pos[0] + 25), y = screenY(layout, pos[1] + 25);
                equal(select(layout, inventory, 0, x, y), rune, "middle click must select rune " + rune);
                equal(select(layout, inventory, 1, x, y), -1, "other deck must not awaken");
                final int hiddenRune = rune;
                equal(RuneAwakeningInput.runeAt(layout, inventory, 0, x, y, slot -> slot != hiddenRune),
                        -1, "search-hidden rune must not awaken");
            }
        }
        layout.update(1280, 669);
        equal(select(layout, inventory, 0, 640, 93), 0, "top-left edge included");
        equal(select(layout, inventory, 0, 689, 142), 0, "last pixel included");
        equal(select(layout, inventory, 0, 690, 100), -1, "right edge excluded");
        equal(select(layout, inventory, 0, 650, 143), -1, "bottom edge excluded");
        equal(select(layout, inventory, 0, 1000, 225), -1, "empty part of last row");
        equal(select(layout, inventory, 0, 650, 338), -1, "purchase label is not a rune target");
        inventory.clear();
        for (int rune = 0; rune < 25; rune++) inventory.set(rune, rune, RuneInventory.NORMAL_RANK);
        equal(select(layout, inventory, 0, 665, 118), -1, "incomplete collection must not awaken");
        fill(inventory, RuneInventory.GOLD_RANK);
        equal(select(layout, inventory, 0, 665, 118), -1, "all gold collection must not awaken");
        inventory.set(25, 25, RuneInventory.SILVER_RANK);
        equal(select(layout, inventory, 0, 940, 228), 25, "mixed ranks remain selectable");
        System.out.println("Awakening input selection, scaling, search and collection guards passed");
    }

    private static int select(GuiLayout layout, RuneInventory inventory, int deck, int x, int y) {
        return RuneAwakeningInput.runeAt(layout, inventory, deck, x, y, slot -> true);
    }
    private static int screenX(GuiLayout l, int x) { return Math.round(l.canvasX + x * l.canvasScale); }
    private static int screenY(GuiLayout l, int y) { return Math.round(l.canvasY + y * l.canvasScale); }
    private static void fill(RuneInventory inventory, int rank) {
        inventory.clear();
        for (int rune = 0; rune < 26; rune++) inventory.set(rune, rune, rank);
    }
    private static void equal(int actual, int expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }
}
