package com.example.revolution.client;

import com.example.revolution.rune.rules.RuneRankRevertDecision;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.gui.GuiScreen;

/** Resolves the existing CTRL + right-click "weaken" gesture for one-step rune rank revert. */
final class RuneRankRevertInput {
    private RuneRankRevertInput() {}

    static boolean isRevertButton(int button) {
        return isRevertGesture(button, GuiScreen.isCtrlKeyDown());
    }

    static boolean isRevertGesture(int button, boolean controlDown) {
        return button == 1 && controlDown;
    }

    static int runeAt(GuiLayout layout, int screenX, int screenY,
                      IntPredicate matchesSearch, IntUnaryOperator rankOf) {
        int x = layout.canvasMouseX(screenX);
        int y = layout.canvasMouseY(screenY);
        for (int rune = 0; rune < com.example.revolution.rune.state.RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int[] pos = layout.runeSlotPosition(rune);
            if (x < pos[0] || x >= pos[0] + layout.runeSlotWidth
                    || y < pos[1] || y >= pos[1] + layout.runeSlotHeight) continue;
            if (!matchesSearch.test(rune)) return -1;
            return RuneRankRevertDecision.canRevertRank(rankOf.applyAsInt(rune)) ? rune : -1;
        }
        return -1;
    }
}
