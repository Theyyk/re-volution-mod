package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

import static com.example.customguimod.GuiDrawing.drawSurface;

/** Screen-local controls; action dispatch and layout remain with the screen. */
final class GuiControls {
    private GuiControls() {}

    static final int PANEL_BORDER_COLOR = 0xFF292B30;
    static final int SLOT_HOVER_COLOR = 0xC0586067;
    static final int SELECTED_COLOR = 0xFF315F9B;
    static final int SELECTED_BG_COLOR = 0xA0152945;
    static final int BUTTON_BG_COLOR = 0x40080808;
    static final int DISABLED_BG_COLOR = 0x20080808;
    static final int TEXT_COLOR = 0xFFFFFFFF;
    static final int DISABLED_TEXT_COLOR = 0xFF909090;
    static final int AMOUNT_BUTTON_START_ID = 1;
    static final int NAV_RUNES_ID = 200;
    static final int CLOSE_BUTTON_ID = 299;

    static class PanelButton extends GuiButton {
        private boolean selected;

        PanelButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int border = selected ? SELECTED_COLOR : (hovered && enabled ? SLOT_HOVER_COLOR : PANEL_BORDER_COLOR);
            int background = selected ? SELECTED_BG_COLOR : (enabled ? BUTTON_BG_COLOR : DISABLED_BG_COLOR);
            int textColor = id == -1 || enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR;

            if (id != CLOSE_BUTTON_ID) drawSurface(x, y, width, height, background, border);

            net.minecraft.client.gui.FontRenderer font = GuiFontRenderer.get(mc);

            if (id == AMOUNT_BUTTON_START_ID) {
                int textWidth = font.getStringWidth(displayString);
                float textScale = Math.min(
                        1.0F,
                        (width - 4) / (float) Math.max(1, textWidth)
                );

                GlStateManager.pushMatrix();
                GlStateManager.translate(
                        x + width / 2.0F,
                        y + (height - font.FONT_HEIGHT * textScale) / 2.0F,
                        0.0F
                );
                GlStateManager.scale(textScale, textScale, 1.0F);
                font.drawString(
                        displayString,
                        -textWidth / 2.0F,
                        0.0F,
                        textColor,
                        false
                );
                GlStateManager.popMatrix();
                return;
            }

            String[] lines = displayString.split("\n");
            int lineHeight = font.FONT_HEIGHT;
            int textY = y + (height - lines.length * lineHeight) / 2;

            for (String line : lines) {
                String label = font.trimStringToWidth(
                        line,
                        Math.max(1, width - 6)
                );
                drawCenteredString(
                        font,
                        label,
                        x + width / 2,
                        textY,
                        textColor
                );
                textY += lineHeight;
            }
        }
    }

    static final class ElementButton extends PanelButton {
        private final String elementName;
        private final boolean active;

        ElementButton(int id, int x, int y, int width, int height,
                      String elementName, boolean active) {
            super(id, x, y, width, height, elementName);
            this.elementName = elementName;
            this.active = active;
            setSelected(active);
        }

        String getElementName() {
            return elementName;
        }

        boolean isActiveElement() {
            return active;
        }
    }

    static final class NavButton extends GuiButton {
        private final ItemStack icon;
        private final boolean selected;

        NavButton(int id, int x, int y, int width, int height,
                  ItemStack icon, boolean selected, boolean enabled) {
            super(id, x, y, width, height, "");
            this.icon = icon;
            this.selected = selected;
            this.enabled = enabled;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int border = selected ? SELECTED_COLOR : (hovered && enabled ? SLOT_HOVER_COLOR : PANEL_BORDER_COLOR);
            int background = selected ? SELECTED_BG_COLOR : (enabled ? BUTTON_BG_COLOR : DISABLED_BG_COLOR);

            if (hovered) drawRect(x, y, x + width, y + height, BUTTON_BG_COLOR);
            if (selected) drawRect(x + width - 1, y, x + width, y + height, SELECTED_COLOR);

            if (id == NAV_RUNES_ID) {
                int cx = x + width / 2, cy = y + height / 2;
                int stone = 0xFF487C86, edge = 0xFF87B6BF, rune = 0xFFD0F4F3;
                drawRect(cx - 7, cy - 8, cx + 7, cy + 8, edge);
                drawRect(cx - 8, cy - 6, cx + 8, cy + 6, edge);
                drawRect(cx - 6, cy - 7, cx + 6, cy + 7, stone);
                drawRect(cx - 7, cy - 5, cx + 7, cy + 5, stone);
                drawRect(cx - 1, cy - 5, cx + 1, cy + 6, rune);
                drawRect(cx + 1, cy - 5, cx + 4, cy - 3, rune);
                drawRect(cx + 3, cy - 3, cx + 5, cy - 1, rune);
                drawRect(cx + 1, cy - 1, cx + 4, cy + 1, rune);
                drawRect(cx - 4, cy + 1, cx - 1, cy + 3, rune);
            }

        }
    }
}
