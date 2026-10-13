package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.*;

/** Screen-local controls; action dispatch and layout remain with the screen. */
final class GuiControls {
    private GuiControls() {}

    static final int AMOUNT_BUTTON_START_ID = 1;
    static final int NAV_RUNES_ID = 200;
    static final int CLOSE_BUTTON_ID = 299;

    static class PanelButton extends GuiButton {
        private static final int RESOURCE_ICON_SIZE = 10;
        private static final int RESOURCE_ICON_GAP = 4;

        private boolean selected;

        PanelButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        private boolean blockedByCardSync() {
            return (id == 0 || id == AMOUNT_BUTTON_START_ID) && CardSyncState.isLoading();
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return !blockedByCardSync() && super.mousePressed(mc, mouseX, mouseY);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            boolean syncBlocked = blockedByCardSync();
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            // A status switch may take a moment while Mongo/server packets arrive. Keep the
            // last meaningful label visible instead of replacing it with a loading placeholder.
            // Interaction is still blocked by mousePressed until CardSyncState settles.
            boolean interactive = enabled && !syncBlocked;
            int border = selected ? SELECTED_COLOR : (hovered && interactive ? SLOT_HOVER_COLOR : PANEL_BORDER_COLOR);
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

            String[] lines = displayString.split("\\n");
            int lineHeight = font.FONT_HEIGHT;
            int textY = y + (height - lines.length * lineHeight) / 2;
            boolean purchasePrice = id == 0
                    && enabled
                    && lines.length > 1
                    && displayString.startsWith("Купить руну");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (purchasePrice && i == 1 && drawPurchasePriceLine(font, line, textY, textColor)) {
                    textY += lineHeight;
                    continue;
                }

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

        private boolean drawPurchasePriceLine(net.minecraft.client.gui.FontRenderer font,
                                              String text, int textY, int textColor) {
            String resource = ResourceManager.normalize(ClientPlayerStats.getPurchaseResource());
            if (!isKnownResource(resource)) return false;

            int textWidth = font.getStringWidth(text);
            int groupWidth = RESOURCE_ICON_SIZE + RESOURCE_ICON_GAP + textWidth;
            if (groupWidth > width - 6) return false;

            int groupX = x + (width - groupWidth) / 2;
            drawResourceIcon(groupX, textY, resource);
            font.drawString(text, groupX + RESOURCE_ICON_SIZE + RESOURCE_ICON_GAP, textY, textColor, false);
            return true;
        }

        private boolean isKnownResource(String resource) {
            return "coins".equals(resource)
                    || "crystals".equals(resource)
                    || "rune_dust".equals(resource)
                    || "rune_shards".equals(resource)
                    || "rune_cores".equals(resource);
        }

        private void drawResourceIcon(int x, int y, String resource) {
            if ("coins".equals(resource)) {
                drawRect(x + 2, y + 1, x + 8, y + 10, COIN_COLOR);
                drawRect(x + 1, y + 3, x + 9, y + 8, COIN_COLOR);
                drawRect(x + 3, y + 3, x + 5, y + 7, COIN_HIGHLIGHT_COLOR);
                return;
            }

            if ("crystals".equals(resource)) {
                drawRect(x + 3, y + 1, x + 7, y + 10, CRYSTAL_COLOR);
                drawRect(x + 1, y + 4, x + 9, y + 7, CRYSTAL_COLOR);
                drawRect(x + 2, y + 2, x + 8, y + 9, CRYSTAL_COLOR);
                drawRect(x + 3, y + 2, x + 5, y + 6, CRYSTAL_HIGHLIGHT_COLOR);
                return;
            }

            if ("rune_dust".equals(resource)) {
                drawRect(x + 1, y + 6, x + 4, y + 9, POISON_TYPE_COLOR);
                drawRect(x + 5, y + 2, x + 7, y + 4, POISON_TYPE_COLOR);
                drawRect(x + 7, y + 6, x + 9, y + 8, CRYSTAL_HIGHLIGHT_COLOR);
                drawRect(x + 3, y + 4, x + 5, y + 6, BOOST_TYPE_COLOR);
                return;
            }

            if ("rune_shards".equals(resource)) {
                drawRect(x + 4, y + 1, x + 7, y + 4, CRYSTAL_COLOR);
                drawRect(x + 3, y + 3, x + 8, y + 7, CRYSTAL_COLOR);
                drawRect(x + 2, y + 6, x + 5, y + 10, BOOST_TYPE_COLOR);
                drawRect(x + 6, y + 5, x + 9, y + 9, CRYSTAL_HIGHLIGHT_COLOR);
                return;
            }

            if ("rune_cores".equals(resource)) {
                drawRect(x + 2, y + 2, x + 8, y + 9, POISON_TYPE_COLOR);
                drawRect(x + 1, y + 4, x + 9, y + 7, POISON_TYPE_COLOR);
                drawRect(x + 4, y + 3, x + 6, y + 8, TYPE_BADGE_BG_COLOR);
                drawRect(x + 3, y + 5, x + 7, y + 7, TYPE_BADGE_BG_COLOR);
                drawRect(x + 4, y + 4, x + 6, y + 6, CRYSTAL_HIGHLIGHT_COLOR);
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

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            boolean pressed = super.mousePressed(mc, mouseX, mouseY);
            if (pressed && !active) CardSyncState.beginLoad();
            return pressed;
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
                int stone = NAV_STONE_COLOR, edge = NAV_EDGE_COLOR, rune = NAV_RUNE_COLOR;
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
