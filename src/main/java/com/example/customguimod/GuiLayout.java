package com.example.customguimod;

/** Logical canvas geometry and input conversion; independent of Minecraft/OpenGL. */
final class GuiLayout {
    static final int CANVAS_WIDTH = 1280;
    static final int CANVAS_HEIGHT = 669;
    private final int visibleRuneSlots;

    private float layoutScale = 1.0F;
    private boolean compactLayout;

    int leftPanelX;
    int leftPanelY;
    int leftPanelWidth;
    int leftPanelHeight;

    int centerPanelX;
    int centerPanelY;
    int centerPanelWidth;
    int centerPanelHeight;

    int runePanelX;
    int runePanelY;
    int runePanelWidth;
    int runePanelHeight;

    int navPanelX;
    int navPanelY;
    int navPanelWidth;
    int navPanelHeight;

    int purchaseTop;
    int purchasePanelY;
    static final int PURCHASE_PANEL_HEIGHT = 102;
    int runeColumns = 10;
    int runeRows = 3;
    int runeSlotWidth;
    int runeSlotHeight;
    int runeSlotGap;
    int runeGridX;
    int runeGridY;

    float canvasScale;
    int canvasX, canvasY;

    GuiLayout(int visibleRuneSlots) {
        this.visibleRuneSlots = visibleRuneSlots;
    }

    int scaled(int base, int minimum) {
        return Math.max(minimum, Math.round(base * layoutScale));
    }

    void update(int width, int height) {
        canvasScale = Math.min(width / (float) CANVAS_WIDTH, height / (float) CANVAS_HEIGHT);
        canvasX = Math.round((width - CANVAS_WIDTH * canvasScale) / 2);
        canvasY = Math.round((height - CANVAS_HEIGHT * canvasScale) / 2);
        layoutScale = 1.0F;
        compactLayout = false;
        leftPanelX = 10; leftPanelY = 10;
        leftPanelWidth = 300; leftPanelHeight = 52;
        centerPanelX = 320; centerPanelY = 10;
        centerPanelWidth = 302; centerPanelHeight = 458;
        runePanelX = 632; runePanelY = 10;
        runePanelWidth = 603; runePanelHeight = 624;
        navPanelWidth = 34;
        navPanelHeight = (int) Math.ceil((height - canvasY) / canvasScale) - 26;
        // Anchor the navigation strip to the actual viewport, including wide windows.
        navPanelX = (int) Math.ceil((width - canvasX) / canvasScale) - navPanelWidth;
        navPanelY = 10;
        updateRuneGridLayout();
    }

    private void updateRuneGridLayout() {
        int pad = scaled(8, 4);
        int topReserved = 83;
        int availableWidth = Math.max(1, runePanelWidth - pad * 2);
        runeSlotGap = scaled(5, 2);
        runeColumns = 10;
        runeRows = (visibleRuneSlots + runeColumns - 1) / runeColumns;
        int sizeByWidth = (availableWidth - runeSlotGap * (runeColumns - 1)) / runeColumns;
        int maxSlot = compactLayout ? scaled(42, 14) : scaled(50, 18);
        runeSlotWidth = Math.max(1, Math.min(sizeByWidth, maxSlot));
        runeSlotHeight = runeSlotWidth;
        runeGridX = runePanelX + pad;
        runeGridY = runePanelY + topReserved;

        int contentHeight = topReserved
                + runeRows * runeSlotHeight
                + (runeRows - 1) * runeSlotGap
                + scaled(29, 20);
        runePanelHeight = contentHeight;
        purchasePanelY = runePanelY + runePanelHeight + 10;
        purchaseTop = purchasePanelY + 28;
    }

    int[] runeSlotPosition(int visualIndex) {
        int row = visualIndex / runeColumns;
        int col = visualIndex % runeColumns;
        return new int[]{
                runeGridX + col * (runeSlotWidth + runeSlotGap),
                runeGridY + row * (runeSlotHeight + runeSlotGap)
        };
    }

    int canvasMouseX(int mouseX) {
        return (int) Math.floor((mouseX - canvasX) / canvasScale);
    }

    int canvasMouseY(int mouseY) {
        return (int) Math.floor((mouseY - canvasY) / canvasScale);
    }
}
