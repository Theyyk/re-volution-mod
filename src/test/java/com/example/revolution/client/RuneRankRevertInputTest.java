package com.example.revolution.client;

import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.state.RuneInventory;
import org.junit.jupiter.api.Test;

/** Exercises CTRL + right-click selection, scaling and search guards for rank revert. */
public final class RuneRankRevertInputTest {
    public static void main(String[] args) {
        run();
    }

    @Test void ctrlRightClickSelectionContract() { run(); }

    public static void run() {
        equal(RuneRankRevertInput.isRevertGesture(1, true), true, "CTRL + right click must revert");
        equal(RuneRankRevertInput.isRevertGesture(1, false), false, "plain right click must not revert");
        equal(RuneRankRevertInput.isRevertGesture(0, true), false, "CTRL + left click must not revert");
        equal(RuneRankRevertInput.isRevertGesture(2, true), false, "CTRL + middle click must not revert");

        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
        int[][] viewports = {{1280, 669}, {1920, 1080}, {2560, 1369}, {640, 360}};
        for (int[] viewport : viewports) {
            layout.update(viewport[0], viewport[1]);
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                int[] pos = layout.runeSlotPosition(rune);
                int x = screenX(layout, pos[0] + 25);
                int y = screenY(layout, pos[1] + 25);
                final int selected = rune;
                equal(RuneRankRevertInput.runeAt(layout, x, y, slot -> true,
                        slot -> slot == selected ? RuneProgression.ADVANCED_RANK : RuneProgression.IMPROVED_RANK),
                        rune, "advanced rune must be selectable for revert");
                equal(RuneRankRevertInput.runeAt(layout, x, y, slot -> slot != selected,
                        slot -> RuneProgression.ADVANCED_RANK),
                        -1, "search-hidden rune must not revert");
                equal(RuneRankRevertInput.runeAt(layout, x, y, slot -> true,
                        slot -> RuneProgression.IMPROVED_RANK),
                        -1, "improved rune must not revert to normal");
            }
        }

        layout.update(1280, 669);
        equal(RuneRankRevertInput.runeAt(layout, 650, 338, slot -> true,
                slot -> RuneProgression.ADVANCED_RANK), -1, "purchase area is not a rune target");
        System.out.println("Rune rank revert CTRL + right-click input, scaling and search guards passed");
    }

    private static int screenX(GuiLayout l, int x) { return Math.round(l.canvasX + x * l.canvasScale); }
    private static int screenY(GuiLayout l, int y) { return Math.round(l.canvasY + y * l.canvasScale); }
    private static void equal(int actual, int expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }
    private static void equal(boolean actual, boolean expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }
}
