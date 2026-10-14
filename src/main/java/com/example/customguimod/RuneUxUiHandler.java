package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.*;

/**
 * v1.2.9 visual feedback for rune progression.
 * Purchase and duplicate rolls use a short soft border pulse, while awakening eligibility uses
 * a stronger slow persistent border breath. Tooltip rendering stays compact and final-value only.
 */
@SideOnly(Side.CLIENT)
public final class RuneUxUiHandler {
    private static final long PURCHASE_PULSE_MS = 450L;
    private static final long[] PURCHASE_PULSE_UNTIL = new long[RuneInventory.BOOST_TYPE_COUNT];

    static void notePurchase(int rune) {
        if (rune < 0 || rune >= PURCHASE_PULSE_UNTIL.length) return;
        PURCHASE_PULSE_UNTIL[rune] = System.currentTimeMillis() + PURCHASE_PULSE_MS;
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof CardsGuiScreen)) return;
        if (!RuneStatusRegistry.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId())) return;

        CardsGuiScreen screen = (CardsGuiScreen) event.getGui();
        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
        layout.update(screen.width, screen.height);

        drawSlotEffects(layout);
        overlayWhiteCtrlHints(layout);
        drawFinalStatTooltip(screen, layout, event.getMouseX(), event.getMouseY());
    }

    private void drawSlotEffects(GuiLayout layout) {
        int awakeningRank = awakeningRank();
        long now = System.currentTimeMillis();
        double wave = (Math.sin(now / 380.0D) + 1.0D) * 0.5D;
        int awakeningAlpha = 135 + (int) Math.round(wave * 100.0D);
        int awakeningColor = withAlpha(SELECTED_COLOR, awakeningAlpha);
        int purchaseColor = withAlpha(TEXT_COLOR, 215);

        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        try {
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                int rank = ClientRuneRanks.rankOf(rune);
                if (rank <= 0) continue;
                int[] pos = layout.runeSlotPosition(rune);

                if (PURCHASE_PULSE_UNTIL[rune] > now) {
                    drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, purchaseColor);
                    continue;
                }

                if (awakeningRank > 0 && rank == awakeningRank) {
                    drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, awakeningColor);
                }
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /** Keeps "+ CTRL" white while the neighboring action description remains muted. */
    private void overlayWhiteCtrlHints(GuiLayout layout) {
        net.minecraft.client.gui.FontRenderer font = GuiFontRenderer.get(Minecraft.getMinecraft());
        String[] labels = {"Пробудить", "+ CTRL Поделиться в чате", "+ CTRL Ослабить"};
        int pad = layout.scaled(8, 4);
        int x = layout.runePanelX + pad;
        int y = layout.runePanelY + layout.runePanelHeight - 17;
        int availableWidth = layout.runePanelWidth - pad * 2;

        int totalWidth = 0;
        for (String label : labels) totalWidth += 13 + font.getStringWidth(label);
        totalWidth += 20;
        float scale = Math.min(1.0F, availableWidth / (float) Math.max(1, totalWidth));

        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        try {
            int offset = 0;
            for (String label : labels) {
                if (label.startsWith("+ CTRL ")) {
                    font.drawString("+ CTRL", offset + 13, 0, TEXT_COLOR, false);
                }
                offset += 13 + font.getStringWidth(label) + 10;
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private int awakeningRank() {
        int minimum = RuneInventory.ADVANCED_RANK;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = ClientRuneRanks.rankOf(rune);
            if (rank <= 0) return 0;
            minimum = Math.min(minimum, rank);
        }
        return minimum >= RuneInventory.ADVANCED_RANK ? 0 : minimum;
    }

    private void drawFinalStatTooltip(CardsGuiScreen screen, GuiLayout layout, int mouseX, int mouseY) {
        if (ClientPlayerStats.getActiveRuneStatusId() != RuneStatusRegistry.STATUS_POISON) return;

        int canvasX = layout.canvasMouseX(mouseX);
        int canvasY = layout.canvasMouseY(mouseY);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = ClientRuneRanks.rankOf(rune);
            if (rank <= 0 || !screen.runeMatchesSearch(rune)) continue;
            int[] pos = layout.runeSlotPosition(rune);
            if (canvasX < pos[0] || canvasX >= pos[0] + layout.runeSlotWidth
                    || canvasY < pos[1] || canvasY >= pos[1] + layout.runeSlotHeight) continue;

            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune);
            if (definition == null) return;

            int enhancement = ClientRuneEnhancements.levelOf(rune);
            double multiplier = ClientRuneEnhancements.multiplierOf(rune);
            List<String> lines = new ArrayList<>();
            lines.add("§b" + definition.name);
            lines.add("§7Ранг: §f" + RuneProgression.rankName(rank));
            lines.add("§7Тип: §f" + definition.type);
            lines.add("§7Улучшение: §f+" + enhancement);

            List<String> stats = RuneStatDisplay.finalValueLines(definition.stats, rank, multiplier);
            if (stats.size() == 1) {
                lines.add("§7Эффекты: §f" + stats.get(0));
            } else if (!stats.isEmpty()) {
                lines.add("§7Эффекты:");
                for (String stat : stats) lines.add("§f" + stat);
            }

            GuiUtils.drawHoveringText(
                    lines,
                    mouseX,
                    mouseY,
                    screen.width,
                    screen.height,
                    -1,
                    GuiFontRenderer.get(Minecraft.getMinecraft())
            );
            return;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
