package com.example.customguimod;

/** Frozen v1.1.5 viewport fixtures and click/slot alignment without Minecraft or OpenGL. */
public final class GuiLayoutTest {
    public static void main(String[] args) {
        // width, height, scale float bits, canvas x/y, nav x/height, mouse (0,0), mouse bottom-right.
        // Captured from the original v1.1.5 screen before extracting GuiLayout.
        int[][] fixtures = {
            {1280, 669, 1065353216, 0, 0, 1246, 643, 0, 0, 1279, 668},
            {2560, 1338, 1073741824, 0, 0, 1246, 643, 0, 0, 1279, 668},
            {1920, 1080, 1069547520, 0, 38, 1246, 669, 0, -26, 1279, 694},
            {854, 480, 1059769549, 0, 17, 1246, 668, 0, -26, 1278, 692},
            {640, 360, 1056964608, 0, 13, 1246, 668, 0, -26, 1278, 692},
            {2560, 1080, 1070506755, 247, 0, 1399, 643, -154, 0, 1432, 668},
            {320, 180, 1048576000, 0, 6, 1246, 670, 0, -24, 1276, 692},
            {1080, 1920, 1062731776, 0, 678, 1246, 1446, 0, -804, 1278, 1470},
            {1279, 668, 1065328138, 0, 0, 1247, 643, 0, 0, 1279, 667},
            {1, 1, 978111693, 0, 0, 1246, 1254, 0, 0, 0, 0}
        };
        // Reuse one layout to also catch stale state across repeated viewport changes.
        GuiLayout layout = new GuiLayout(26);
        for (int pass = 0; pass < 3; pass++) {
            for (int[] f : fixtures) {
                layout.update(f[0], f[1]);
                String viewport = f[0] + "x" + f[1];
                equal(Float.floatToIntBits(layout.canvasScale), f[2], viewport + " scale");
                equal(layout.canvasX, f[3], viewport + " canvas X");
                equal(layout.canvasY, f[4], viewport + " canvas Y");
                equal(layout.navPanelX, f[5], viewport + " nav X");
                equal(layout.navPanelHeight, f[6], viewport + " nav height");
                equal(layout.canvasMouseX(0), f[7], viewport + " left input");
                equal(layout.canvasMouseY(0), f[8], viewport + " top input");
                equal(layout.canvasMouseX(f[0] - 1), f[9], viewport + " right input");
                equal(layout.canvasMouseY(f[1] - 1), f[10], viewport + " bottom input");
                checkGeometry(layout);
                if (f[0] >= 320 && f[1] >= 180) checkSlotClicks(layout);
            }
        }
        System.out.println("GUI layout goldens, repeated resize and all 26 rune click targets passed");
    }

    private static void checkGeometry(GuiLayout l) {
        equal(l.leftPanelX, 10, "left X"); equal(l.leftPanelY, 10, "left Y");
        equal(l.leftPanelWidth, 300, "left width"); equal(l.leftPanelHeight, 52, "left height");
        equal(l.centerPanelX, 320, "center X"); equal(l.centerPanelY, 10, "center Y");
        equal(l.centerPanelWidth, 302, "center width"); equal(l.centerPanelHeight, 458, "center height");
        equal(l.runePanelX, 632, "rune panel X"); equal(l.runePanelY, 10, "rune panel Y");
        equal(l.runePanelWidth, 603, "rune panel width"); equal(l.runePanelHeight, 272, "rune panel height");
        equal(l.navPanelY, 10, "nav Y"); equal(l.navPanelWidth, 34, "nav width");
        equal(l.runeColumns, 10, "columns"); equal(l.runeRows, 3, "rows");
        equal(l.runeSlotWidth, 50, "slot width"); equal(l.runeSlotHeight, 50, "slot height");
        equal(l.runeSlotGap, 5, "slot gap");
        equal(l.purchasePanelY, 292, "purchase panel Y"); equal(l.purchaseTop, 320, "purchase origin");
        equal(GuiLayout.PURCHASE_PANEL_HEIGHT, 102, "purchase height");
        int[][] slots = {{0, 640, 93}, {9, 1135, 93}, {10, 640, 148}, {25, 915, 203}};
        for (int[] slot : slots) {
            int[] pos = l.runeSlotPosition(slot[0]);
            equal(pos[0], slot[1], "slot " + slot[0] + " X");
            equal(pos[1], slot[2], "slot " + slot[0] + " Y");
        }
    }

    private static void checkSlotClicks(GuiLayout l) {
        for (int slot = 0; slot < 26; slot++) {
            int[] pos = l.runeSlotPosition(slot);
            int screenX = Math.round(l.canvasX + (pos[0] + 25) * l.canvasScale);
            int screenY = Math.round(l.canvasY + (pos[1] + 25) * l.canvasScale);
            int x = l.canvasMouseX(screenX), y = l.canvasMouseY(screenY);
            if (x < pos[0] || x >= pos[0] + 50 || y < pos[1] || y >= pos[1] + 50) {
                throw new AssertionError("Rendered rune center misses click target " + slot);
            }
        }
    }

    private static void equal(int actual, int expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + actual + " != " + expected);
    }
}
