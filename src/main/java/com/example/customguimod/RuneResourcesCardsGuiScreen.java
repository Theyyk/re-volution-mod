package com.example.customguimod;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.*;

/**
 * v1.2.2 presentation layer for persistent rune resource balances.
 * Currency and rune upgrade materials are intentionally shown as separate groups.
 */
public final class RuneResourcesCardsGuiScreen extends CardsGuiScreen {

    private static final int CURRENCY_PANEL_HEIGHT = 68;
    private static final int RUNE_PANEL_HEIGHT = 84;
    private static final int PANEL_GAP = 11;
    private static final int CURRENCY_HEADING_MASK = 0x90080808;
    private static final int RESOURCE_ICON_SIZE = 10;
    private static final int RESOURCE_ICON_GAP = 5;

    private final GuiLayout resourceLayout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        resourceLayout.update(width, height);
        GlStateManager.pushMatrix();
        GlStateManager.translate(resourceLayout.canvasX, resourceLayout.canvasY, 0);
        GlStateManager.scale(resourceLayout.canvasScale, resourceLayout.canvasScale, 1);

        int panelX = resourceLayout.leftPanelX;
        int currencyY = resourceLayout.leftPanelY + resourceLayout.leftPanelHeight + 11;
        int runeY = currencyY + CURRENCY_PANEL_HEIGHT + PANEL_GAP;
        int panelWidth = resourceLayout.leftPanelWidth;
        int pad = resourceLayout.scaled(8, 4);
        int textWidth = Math.max(10, panelWidth - pad * 2);
        int x = panelX + pad;

        drawSurface(panelX, currencyY, panelWidth, CURRENCY_PANEL_HEIGHT,
                0x00000000, PANEL_BORDER_COLOR);
        Gui.drawRect(panelX + 2, currencyY + 2,
                panelX + panelWidth - 2, currencyY + pad + 13, CURRENCY_HEADING_MASK);

        int y = currencyY + pad;
        GuiFontRenderer.heading(mc).drawStringWithShadow("ВАЛЮТА", x, y, HEADING_TEXT_COLOR);
        y += resourceLayout.scaled(21, 16);
        drawResourceLine(x, y, "Монеты", ClientPlayerStats.getCoins(), textWidth);
        y += resourceLayout.scaled(16, 12);
        drawResourceLine(x, y, "Кристаллы", ClientPlayerStats.getCrystals(), textWidth);

        drawSurface(panelX, runeY, panelWidth, RUNE_PANEL_HEIGHT,
                PANEL_BG_COLOR, PANEL_BORDER_COLOR);

        y = runeY + pad;
        GuiFontRenderer.heading(mc).drawStringWithShadow("РЕСУРСЫ РУН", x, y, HEADING_TEXT_COLOR);
        y += resourceLayout.scaled(21, 16);
        drawResourceLine(x, y, "Пыль рун", ClientPlayerStats.getRuneDust(), textWidth);
        y += resourceLayout.scaled(16, 12);
        drawResourceLine(x, y, "Осколки рун", ClientPlayerStats.getRuneShards(), textWidth);
        y += resourceLayout.scaled(16, 12);
        drawResourceLine(x, y, "Ядра рун", ClientPlayerStats.getRuneCores(), textWidth);

        GlStateManager.popMatrix();
    }

    private void drawResourceLine(int x, int y, String name, int value, int availableWidth) {
        String valueText = String.valueOf(value);
        int digitAdvance = stableDigitAdvance();
        int valueWidth = stableValueWidth(valueText, digitAdvance);
        int valueX = x + availableWidth - valueWidth;
        int nameX = x + RESOURCE_ICON_SIZE + RESOURCE_ICON_GAP;
        int nameMaxWidth = Math.max(8, valueX - nameX - resourceLayout.scaled(4, 2));

        drawResourceIcon(x, y, name);
        drawFitted(name, nameX, y, nameMaxWidth);
        drawStableValue(valueText, valueX, y, digitAdvance);
    }

    private int stableDigitAdvance() {
        int widest = 0;
        for (char digit = '0'; digit <= '9'; digit++) {
            widest = Math.max(widest, fontRenderer.getCharWidth(digit));
        }
        return Math.max(1, widest);
    }

    private int stableValueWidth(String text, int digitAdvance) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            width += Character.isDigit(c) ? digitAdvance : fontRenderer.getCharWidth(c);
        }
        return width;
    }

    private void drawStableValue(String text, int x, int y, int digitAdvance) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String glyph = String.valueOf(c);
            int glyphWidth = fontRenderer.getStringWidth(glyph);
            int advance = Character.isDigit(c) ? digitAdvance : glyphWidth;
            int glyphX = cursor + Math.max(0, (advance - glyphWidth) / 2);
            fontRenderer.drawString(glyph, glyphX, y, TEXT_COLOR);
            cursor += Math.max(1, advance);
        }
    }

    private void drawResourceIcon(int x, int y, String name) {
        if ("Монеты".equals(name)) {
            drawRect(x + 2, y + 1, x + 8, y + 10, COIN_COLOR);
            drawRect(x + 1, y + 3, x + 9, y + 8, COIN_COLOR);
            drawRect(x + 3, y + 3, x + 5, y + 7, COIN_HIGHLIGHT_COLOR);
            return;
        }

        if ("Кристаллы".equals(name)) {
            drawRect(x + 3, y + 1, x + 7, y + 10, CRYSTAL_COLOR);
            drawRect(x + 1, y + 4, x + 9, y + 7, CRYSTAL_COLOR);
            drawRect(x + 2, y + 2, x + 8, y + 9, CRYSTAL_COLOR);
            drawRect(x + 3, y + 2, x + 5, y + 6, CRYSTAL_HIGHLIGHT_COLOR);
            return;
        }

        if ("Пыль рун".equals(name)) {
            drawRect(x + 1, y + 6, x + 4, y + 9, POISON_TYPE_COLOR);
            drawRect(x + 5, y + 2, x + 7, y + 4, POISON_TYPE_COLOR);
            drawRect(x + 7, y + 6, x + 9, y + 8, CRYSTAL_HIGHLIGHT_COLOR);
            drawRect(x + 3, y + 4, x + 5, y + 6, BOOST_TYPE_COLOR);
            return;
        }

        if ("Осколки рун".equals(name)) {
            drawRect(x + 4, y + 1, x + 7, y + 4, CRYSTAL_COLOR);
            drawRect(x + 3, y + 3, x + 8, y + 7, CRYSTAL_COLOR);
            drawRect(x + 2, y + 6, x + 5, y + 10, BOOST_TYPE_COLOR);
            drawRect(x + 6, y + 5, x + 9, y + 9, CRYSTAL_HIGHLIGHT_COLOR);
            return;
        }

        if ("Ядра рун".equals(name)) {
            drawRect(x + 2, y + 2, x + 8, y + 9, POISON_TYPE_COLOR);
            drawRect(x + 1, y + 4, x + 9, y + 7, POISON_TYPE_COLOR);
            drawRect(x + 4, y + 3, x + 6, y + 8, TYPE_BADGE_BG_COLOR);
            drawRect(x + 3, y + 5, x + 7, y + 7, TYPE_BADGE_BG_COLOR);
            drawRect(x + 4, y + 4, x + 6, y + 6, CRYSTAL_HIGHLIGHT_COLOR);
        }
    }

    private void drawFitted(String text, int x, int y, int maxWidth) {
        int width = fontRenderer.getStringWidth(text);
        if (width <= maxWidth) {
            fontRenderer.drawString(text, x, y, TEXT_COLOR);
            return;
        }

        float scale = Math.max(0.55F, maxWidth / (float) Math.max(1, width));
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        fontRenderer.drawString(text, 0, 0, TEXT_COLOR);
        GlStateManager.popMatrix();
    }
}
