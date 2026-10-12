package com.example.customguimod;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.HEADING_TEXT_COLOR;
import static com.example.customguimod.GuiTheme.PANEL_BG_COLOR;
import static com.example.customguimod.GuiTheme.PANEL_BORDER_COLOR;
import static com.example.customguimod.GuiTheme.TEXT_COLOR;

/**
 * v1.2.2 presentation layer for persistent rune resource balances.
 * Currency and rune upgrade materials are intentionally shown as separate groups.
 */
public final class RuneResourcesCardsGuiScreen extends CardsGuiScreen {

    private static final int CURRENCY_PANEL_HEIGHT = 68;
    private static final int RUNE_PANEL_HEIGHT = 84;
    private static final int PANEL_GAP = 11;
    private static final int PANEL_CLEAR_COLOR = 0xFF080808;

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

        // Replace the original single resource block with the agreed two-group layout.
        Gui.drawRect(panelX, currencyY, panelX + panelWidth, currencyY + CURRENCY_PANEL_HEIGHT,
                PANEL_CLEAR_COLOR);
        drawSurface(panelX, currencyY, panelWidth, CURRENCY_PANEL_HEIGHT,
                PANEL_BG_COLOR, PANEL_BORDER_COLOR);

        int y = currencyY + pad;
        GuiFontRenderer.heading(mc).drawStringWithShadow("ВАЛЮТА", x, y, HEADING_TEXT_COLOR);
        y += resourceLayout.scaled(21, 16);
        drawResourceLine(x, y, "Монеты", ClientPlayerStats.getCoins(), textWidth);
        y += resourceLayout.scaled(16, 12);
        drawResourceLine(x, y, "Кристаллы", ClientPlayerStats.getCrystals(), textWidth);

        Gui.drawRect(panelX, runeY, panelX + panelWidth, runeY + RUNE_PANEL_HEIGHT,
                PANEL_CLEAR_COLOR);
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
        int valueX = x + availableWidth - fontRenderer.getStringWidth(valueText);
        int nameMaxWidth = Math.max(8, valueX - x - resourceLayout.scaled(4, 2));

        drawFitted(name, x, y, nameMaxWidth);
        fontRenderer.drawString(valueText, valueX, y, TEXT_COLOR);
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
