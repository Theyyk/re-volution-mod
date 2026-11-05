package com.example.revolution.client;

/** Minecraft-independent geometry for non-rune E-menu screens. */
final class UnifiedMainMenuGeometry {
    private UnifiedMainMenuGeometry() {}

    static int contentLeft(int width, int height) {
        GuiLayout layout = new GuiLayout(26);
        layout.update(width, height);
        return layout.canvasX + Math.round(layout.centerPanelX * layout.canvasScale)
                + Math.max(1, Math.round(8 * layout.canvasScale));
    }

    static int contentRight(int width, int height) {
        GuiLayout layout = new GuiLayout(26);
        layout.update(width, height);
        return layout.canvasX + Math.round(layout.navPanelX * layout.canvasScale)
                - Math.max(1, Math.round(12 * layout.canvasScale));
    }

    static int contentTop(int width, int height) {
        GuiLayout layout = new GuiLayout(26);
        layout.update(width, height);
        return layout.canvasY + Math.round(65 * layout.canvasScale);
    }
}
