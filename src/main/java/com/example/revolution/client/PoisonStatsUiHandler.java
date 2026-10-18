package com.example.revolution.client;

import com.example.revolution.rune.RuneInventory;
import com.example.revolution.number.LargeNumbers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.example.revolution.client.GuiTheme.PANEL_BG_COLOR;
import static com.example.revolution.client.GuiTheme.POISON_TYPE_COLOR;
import static com.example.revolution.client.GuiTheme.TEXT_COLOR;

/** Draws server-resolved total combat values from all configured rune collections. */
@SideOnly(Side.CLIENT)
public final class PoisonStatsUiHandler {
    private static final int STAT_ICON_SIZE = 10;
    private static final int STAT_ICON_GAP = 5;

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;

        RunesGuiScreen screen = (RunesGuiScreen) event.getGui();
        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
        layout.update(screen.width, screen.height);

        FontRenderer font = GuiFontRenderer.get(Minecraft.getMinecraft());
        int pad = layout.scaled(8, 4);
        int x = layout.leftPanelX + pad;
        int clickY = layout.leftPanelY + pad + layout.scaled(21, 16);
        int poisonY = clickY + 13;
        int availableWidth = Math.max(10, layout.leftPanelWidth - pad * 2);
        int valueAreaX = x + availableWidth / 2;
        int valueRight = x + availableWidth;

        String clickValue = LargeNumbers.format(ClientPlayerStats.getResolvedClickDamage());
        String poisonValue = LargeNumbers.format(ClientPlayerStats.getResolvedPoisonDamage());

        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        try {
            // Base screen still paints the old int-sized value. Replace that area with an opaque copy
            // of the panel background so the legacy digits cannot remain visible underneath.
            int opaquePanel = 0xFF000000 | (PANEL_BG_COLOR & 0x00FFFFFF);
            Gui.drawRect(valueAreaX, clickY - 1, valueRight + 1, clickY + font.FONT_HEIGHT + 1, opaquePanel);
            drawRightAligned(font, clickValue, valueRight, clickY);

            drawPoisonIcon(x, poisonY);
            font.drawString("Урон яда", x + STAT_ICON_SIZE + STAT_ICON_GAP, poisonY, TEXT_COLOR, false);
            drawRightAligned(font, poisonValue, valueRight, poisonY);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static void drawRightAligned(FontRenderer font, String value, int right, int y) {
        font.drawString(value, right - font.getStringWidth(value), y, TEXT_COLOR, false);
    }

    private static void drawPoisonIcon(int x, int y) {
        Gui.drawRect(x + 4, y, x + 6, y + 2, POISON_TYPE_COLOR);
        Gui.drawRect(x + 3, y + 2, x + 7, y + 4, POISON_TYPE_COLOR);
        Gui.drawRect(x + 2, y + 4, x + 8, y + 7, POISON_TYPE_COLOR);
        Gui.drawRect(x + 3, y + 7, x + 7, y + 9, POISON_TYPE_COLOR);
    }
}
