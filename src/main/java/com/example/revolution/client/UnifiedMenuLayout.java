package com.example.revolution.client;

/**
 * Right-hand E-menu tab placements, using the same virtual 1280x669
 * coordinate system as the original Rune GUI.
 */
final class UnifiedMenuLayout {
    static final int VERTICAL_GAP = 3;
    private UnifiedMenuLayout() {}

    static int size(GuiLayout layout) {
        return layout.navPanelWidth - 4;
    }

    static int x(GuiLayout layout) {
        return layout.navPanelX + 2;
    }

    static int y(GuiLayout layout, int index) {
        if (index < 0) throw new IllegalArgumentException("negative tab index");
        return layout.navPanelY + 36 + index * (size(layout) + VERTICAL_GAP);
    }
}
