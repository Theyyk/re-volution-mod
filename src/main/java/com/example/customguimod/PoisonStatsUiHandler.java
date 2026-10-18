package com.example.customguimod;

import com.example.customguimod.number.LargeNumbers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.example.customguimod.GuiTheme.POISON_TYPE_COLOR;
import static com.example.customguimod.GuiTheme.TEXT_COLOR;

/** Draws the current stable poison damage directly under click damage in the rune GUI statistics panel. */
@SideOnly(Side.CLIENT)
public final class PoisonStatsUiHandler {
    private static final int STAT_ICON_SIZE = 10;
    private static final int STAT_ICON_GAP = 5;

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        if (ClientPlayerStats.getActiveRuneStatusId() != RuneStatusRegistry.STATUS_POISON) return;

        RunesGuiScreen screen = (RunesGuiScreen) event.getGui();
        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
        layout.update(screen.width, screen.height);

        FontRenderer font = GuiFontRenderer.get(Minecraft.getMinecraft());
        int pad = layout.scaled(8, 4);
        int x = layout.leftPanelX + pad;
        int y = layout.leftPanelY + pad + layout.scaled(21, 16) + 13;
        int availableWidth = Math.max(10, layout.leftPanelWidth - pad * 2);
        String value = LargeNumbers.format(ClientRuneCombatPreview.poisonDamage());
        int valueWidth = font.getStringWidth(value);
        int valueX = x + availableWidth - valueWidth;
        int nameX = x + STAT_ICON_SIZE + STAT_ICON_GAP;

        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        try {
            drawPoisonIcon(x, y);
            font.drawString("Урон яда", nameX, y, TEXT_COLOR, false);
            font.drawString(value, valueX, y, TEXT_COLOR, false);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static void drawPoisonIcon(int x, int y) {
        Gui.drawRect(x + 4, y, x + 6, y + 2, POISON_TYPE_COLOR);
        Gui.drawRect(x + 3, y + 2, x + 7, y + 4, POISON_TYPE_COLOR);
        Gui.drawRect(x + 2, y + 4, x + 8, y + 7, POISON_TYPE_COLOR);
        Gui.drawRect(x + 3, y + 7, x + 7, y + 9, POISON_TYPE_COLOR);
    }
}
