package com.example.revolution.client.hud;

import com.example.revolution.client.ClientPlayerStats;
import com.example.revolution.number.LargeNumbers;
import java.math.BigDecimal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Minimal RE:volution HUD that replaces vanilla Survival status chrome. */
@SideOnly(Side.CLIENT)
public final class RpgHudRenderer {
    private static final int PANEL_BG = 0xA8181D24;
    private static final int PANEL_EDGE = 0xB05E8FB3;
    private static final int TEXT = 0xE8EEF2;
    private static final int COINS = 0xD9B34A;
    private static final int CRYSTALS = 0x66C8E8;
    private static final int DUST = 0x9B7BD3;
    private static final int SHARDS = 0x65B9D6;
    private static final int CORES = 0xD36B8A;
    private static final int DAMAGE = 0xD98272;
    private static final int POISON = 0x79B86A;

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.playerController == null || minecraft.gameSettings.hideGUI) return;
        if (minecraft.playerController.getCurrentGameType() != GameType.SURVIVAL) return;

        renderCombat(minecraft, event.getResolution());
        renderResources(minecraft, event.getResolution());
    }

    private static void renderCombat(Minecraft minecraft, ScaledResolution resolution) {
        FontRenderer font = minecraft.fontRenderer;
        String click = LargeNumbers.format(ClientPlayerStats.getResolvedClickDamage());
        String poison = LargeNumbers.format(ClientPlayerStats.getResolvedPoisonDamage());
        int width = Math.max(font.getStringWidth(click), font.getStringWidth(poison)) + 28;
        int x = 8;
        int y = 8;

        drawPanel(x, y, x + width, y + 34);
        drawSwordIcon(x + 7, y + 6, DAMAGE);
        font.drawStringWithShadow(click, x + 22, y + 6, TEXT);
        drawPoisonIcon(x + 7, y + 20, POISON);
        font.drawStringWithShadow(poison, x + 22, y + 20, TEXT);
    }

    private static void renderResources(Minecraft minecraft, ScaledResolution resolution) {
        FontRenderer font = minecraft.fontRenderer;
        ResourceView[] values = new ResourceView[] {
                new ResourceView("$", ClientPlayerStats.getCoins(), COINS),
                new ResourceView("*", ClientPlayerStats.getCrystals(), CRYSTALS),
                new ResourceView("D", ClientPlayerStats.getRuneDust(), DUST),
                new ResourceView("S", ClientPlayerStats.getRuneShards(), SHARDS),
                new ResourceView("C", ClientPlayerStats.getRuneCores(), CORES)
        };

        int contentWidth = 0;
        for (ResourceView value : values) {
            value.text = LargeNumbers.format(value.value);
            contentWidth += 14 + font.getStringWidth(value.text) + 12;
        }
        int panelWidth = contentWidth + 8;
        int panelHeight = 22;
        int x = (resolution.getScaledWidth() - panelWidth) / 2;
        int y = resolution.getScaledHeight() - panelHeight - 7;
        drawPanel(x, y, x + panelWidth, y + panelHeight);

        int cursor = x + 7;
        for (ResourceView value : values) {
            drawBadge(font, value.symbol, cursor, y + 6, value.color);
            cursor += 14;
            font.drawStringWithShadow(value.text, cursor, y + 7, TEXT);
            cursor += font.getStringWidth(value.text) + 12;
        }
    }

    private static void drawPanel(int left, int top, int right, int bottom) {
        Gui.drawRect(left, top, right, bottom, PANEL_BG);
        Gui.drawRect(left, top, right, top + 1, PANEL_EDGE);
        Gui.drawRect(left, bottom - 1, right, bottom, 0x702A323C);
    }

    private static void drawBadge(FontRenderer font, String symbol, int x, int y, int color) {
        Gui.drawRect(x, y, x + 10, y + 10, 0xB0101419);
        Gui.drawRect(x + 1, y + 1, x + 9, y + 9, 0xC0000000 | color);
        int textX = x + (10 - font.getStringWidth(symbol)) / 2;
        font.drawString(symbol, textX, y + 1, 0xFFFFFFFF);
    }

    private static void drawSwordIcon(int x, int y, int color) {
        int c = 0xFF000000 | color;
        Gui.drawRect(x + 7, y, x + 9, y + 2, c);
        Gui.drawRect(x + 5, y + 2, x + 8, y + 4, c);
        Gui.drawRect(x + 3, y + 4, x + 6, y + 6, c);
        Gui.drawRect(x + 1, y + 6, x + 4, y + 8, c);
        Gui.drawRect(x, y + 8, x + 5, y + 9, 0xFFE1E5E8);
        Gui.drawRect(x + 1, y + 9, x + 3, y + 11, 0xFFE1E5E8);
    }

    private static void drawPoisonIcon(int x, int y, int color) {
        int c = 0xFF000000 | color;
        Gui.drawRect(x + 4, y, x + 6, y + 3, c);
        Gui.drawRect(x + 2, y + 3, x + 8, y + 8, c);
        Gui.drawRect(x + 3, y + 8, x + 7, y + 10, c);
    }

    private static final class ResourceView {
        private final String symbol;
        private final BigDecimal value;
        private final int color;
        private String text;

        private ResourceView(String symbol, BigDecimal value, int color) {
            this.symbol = symbol;
            this.value = value;
            this.color = color;
        }
    }
}
