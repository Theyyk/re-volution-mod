package com.example.customguimod;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

/** Draws rune tooltips as a compact framed card instead of the vanilla tooltip box. */
final class RuneTooltipRenderer {
    private static final int BACKGROUND = 0xF21A1C20;
    private static final int NORMAL_BORDER = 0xFF555A61;
    private static final int NORMAL_ACCENT = 0xFF747A82;
    private static final int IMPROVED_BORDER = 0xFFC9CDD2;
    private static final int IMPROVED_ACCENT = 0xFFE4E8EF;
    private static final int ADVANCED_BORDER = 0xFFE0B64A;
    private static final int ADVANCED_ACCENT = 0xFFF7D477;
    private static final int PADDING_X = 8;
    private static final int PADDING_TOP = 7;
    private static final int PADDING_BOTTOM = 7;
    private static final int LINE_GAP = 2;
    private static final int TITLE_GAP = 5;
    private static final int CORNER_LENGTH = 8;

    private RuneTooltipRenderer() {}

    static void draw(List<String> lines,
                     int rank,
                     int mouseX,
                     int mouseY,
                     int screenWidth,
                     int screenHeight,
                     FontRenderer font) {
        if (lines == null || lines.isEmpty() || font == null) return;

        int borderColor = borderColor(rank);
        int accentColor = accentColor(rank);

        int textWidth = 0;
        for (String line : lines) {
            if (line == null) continue;
            textWidth = Math.max(textWidth, font.getStringWidth(line));
        }

        int lineHeight = font.FONT_HEIGHT + LINE_GAP;
        int bodyLineCount = Math.max(0, lines.size() - 1);
        int width = Math.max(118, textWidth + PADDING_X * 2);
        int height = PADDING_TOP + font.FONT_HEIGHT + TITLE_GAP + 1
                + (bodyLineCount == 0 ? 0 : 5 + bodyLineCount * lineHeight - LINE_GAP)
                + PADDING_BOTTOM;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + 5 > screenWidth) x = mouseX - width - 12;
        if (x < 5) x = 5;
        if (y + height + 5 > screenHeight) y = screenHeight - height - 5;
        if (y < 5) y = 5;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        try {
            Gui.drawRect(x, y, x + width, y + height, BACKGROUND);
            drawBorder(x, y, width, height, borderColor);
            drawCornerAccents(x, y, width, height, accentColor);

            int textX = x + PADDING_X;
            int titleY = y + PADDING_TOP;
            font.drawStringWithShadow(lines.get(0), textX, titleY, 0xFFFFFFFF);

            int dividerY = titleY + font.FONT_HEIGHT + TITLE_GAP;
            Gui.drawRect(textX, dividerY, x + width - PADDING_X, dividerY + 1, borderColor);

            int textY = dividerY + 6;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line != null && !line.isEmpty()) {
                    font.drawStringWithShadow(line, textX, textY, 0xFFFFFFFF);
                }
                textY += lineHeight;
            }
        } finally {
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    private static int borderColor(int rank) {
        if (rank >= RuneInventory.ADVANCED_RANK) return ADVANCED_BORDER;
        if (rank >= RuneInventory.IMPROVED_RANK) return IMPROVED_BORDER;
        return NORMAL_BORDER;
    }

    private static int accentColor(int rank) {
        if (rank >= RuneInventory.ADVANCED_RANK) return ADVANCED_ACCENT;
        if (rank >= RuneInventory.IMPROVED_RANK) return IMPROVED_ACCENT;
        return NORMAL_ACCENT;
    }

    private static void drawBorder(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + 1, color);
        Gui.drawRect(x, y + height - 1, x + width, y + height, color);
        Gui.drawRect(x, y, x + 1, y + height, color);
        Gui.drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawCornerAccents(int x, int y, int width, int height, int color) {
        int right = x + width;
        int bottom = y + height;

        Gui.drawRect(x + 2, y + 2, x + 2 + CORNER_LENGTH, y + 3, color);
        Gui.drawRect(x + 2, y + 2, x + 3, y + 2 + CORNER_LENGTH, color);

        Gui.drawRect(right - 2 - CORNER_LENGTH, y + 2, right - 2, y + 3, color);
        Gui.drawRect(right - 3, y + 2, right - 2, y + 2 + CORNER_LENGTH, color);

        Gui.drawRect(x + 2, bottom - 3, x + 2 + CORNER_LENGTH, bottom - 2, color);
        Gui.drawRect(x + 2, bottom - 2 - CORNER_LENGTH, x + 3, bottom - 2, color);

        Gui.drawRect(right - 2 - CORNER_LENGTH, bottom - 3, right - 2, bottom - 2, color);
        Gui.drawRect(right - 3, bottom - 2 - CORNER_LENGTH, right - 2, bottom - 2, color);
    }
}
