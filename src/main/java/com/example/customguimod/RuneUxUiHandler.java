package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.math.BigDecimal;
import java.util.List;

import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.*;

@SideOnly(Side.CLIENT)
public final class RuneUxUiHandler {
    private static final long PURCHASE_PULSE_MS = 450L;
    private static final long ENHANCEMENT_PULSE_MS = 650L;
    private static final int IMPROVED_SILVER = 0x00E4E8EF;
    private static final int ADVANCED_GOLD = 0x00F7D477;
    private static final int ENHANCEMENT_GREEN = 0x006FD080;
    private static final int PURCHASE_CYAN = 0x007FC9E8;
    private static final long[] PURCHASE_PULSE_UNTIL = new long[RuneInventory.BOOST_TYPE_COUNT];
    private static final long[] ENHANCEMENT_PULSE_UNTIL = new long[RuneInventory.BOOST_TYPE_COUNT];

    static void notePurchase(int rune) { if (rune >= 0 && rune < PURCHASE_PULSE_UNTIL.length) PURCHASE_PULSE_UNTIL[rune] = System.currentTimeMillis() + PURCHASE_PULSE_MS; }
    static void noteEnhancement(int rune) { if (rune >= 0 && rune < ENHANCEMENT_PULSE_UNTIL.length) ENHANCEMENT_PULSE_UNTIL[rune] = System.currentTimeMillis() + ENHANCEMENT_PULSE_MS; }

    @SubscribeEvent public void onLegacyRuneTooltip(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof CardsGuiScreen)) return;
        if (ClientPlayerStats.getActiveRuneStatusId() != RuneStatusRegistry.STATUS_POISON) return;
        List<String> lines = event.getLines(); if (lines == null || lines.isEmpty()) return;
        boolean hasRank = false, hasType = false;
        for (String line : lines) { if (line == null) continue; if (line.contains("Ранг:")) hasRank = true; if (line.contains("Тип:")) hasType = true; }
        if (hasRank && hasType) event.setCanceled(true);
    }

    @SubscribeEvent public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof CardsGuiScreen)) return;
        if (!RuneStatusRegistry.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId())) return;
        CardsGuiScreen screen = (CardsGuiScreen) event.getGui();
        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT); layout.update(screen.width, screen.height);
        drawSlotEffects(layout); overlayWhiteCtrlHints(layout); drawFinalStatTooltip(screen, layout, event.getMouseX(), event.getMouseY());
    }

    private void drawSlotEffects(GuiLayout layout) {
        int eligibleRank = awakeningRank(); long now = System.currentTimeMillis();
        double wave = (Math.sin(now / 300.0D) + 1.0D) * 0.5D;
        int markerAlpha = 95 + (int) Math.round(wave * 160.0D);
        int purchaseColor = withAlpha(PURCHASE_CYAN, 195), enhancementColor = withAlpha(ENHANCEMENT_GREEN, 190);
        GlStateManager.pushMatrix(); GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F); GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        try {
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                int rank = ClientRuneRanks.rankOf(rune); int[] pos = layout.runeSlotPosition(rune);
                if (rank >= RuneInventory.IMPROVED_RANK) eraseLegacyRankCorner(pos[0], pos[1], layout.runeSlotWidth, layout);
                if (rank >= RuneInventory.ADVANCED_RANK) drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, withAlpha(ADVANCED_GOLD, 165));
                else if (rank >= RuneInventory.IMPROVED_RANK) drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, withAlpha(IMPROVED_SILVER, 150));
                if (ENHANCEMENT_PULSE_UNTIL[rune] > now) drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, enhancementColor);
                else if (rank > 0 && PURCHASE_PULSE_UNTIL[rune] > now) drawSurface(pos[0], pos[1], layout.runeSlotWidth, layout.runeSlotHeight, 0, purchaseColor);
                if (rank > 0 && eligibleRank > 0 && rank == eligibleRank && canAffordAwakening(rank)) {
                    int markerBase = rank >= RuneInventory.IMPROVED_RANK ? ADVANCED_GOLD : IMPROVED_SILVER;
                    drawAwakeningMarker(pos[0], pos[1], withAlpha(markerBase, markerAlpha));
                }
            }
        } finally { GlStateManager.popMatrix(); }
    }

    private void eraseLegacyRankCorner(int slotX, int slotY, int slotWidth, GuiLayout layout) {
        int length = Math.max(3, Math.min(layout.scaled(8, 4), slotWidth / 5)); int bg = withAlpha(SLOT_BG_COLOR, 255);
        net.minecraft.client.gui.Gui.drawRect(slotX + 2, slotY + 2, slotX + 2 + length, slotY + 3, bg);
        net.minecraft.client.gui.Gui.drawRect(slotX + 2, slotY + 2, slotX + 3, slotY + 2 + length, bg);
    }
    private void drawAwakeningMarker(int slotX, int slotY, int color) {
        int cx = slotX + 7, cy = slotY + 7;
        net.minecraft.client.gui.Gui.drawRect(cx - 1, cy - 5, cx + 2, cy + 6, color);
        net.minecraft.client.gui.Gui.drawRect(cx - 5, cy - 1, cx + 6, cy + 2, color);
        net.minecraft.client.gui.Gui.drawRect(cx - 3, cy - 3, cx + 4, cy + 4, color);
    }

    private boolean canAffordAwakening(int rank) {
        String resource = ClientPlayerStats.getAwakeningResource(); int cost = ClientPlayerStats.getAwakeningCostForRank(rank);
        if (resource == null || resource.trim().isEmpty() || cost <= 0) return true;
        BigDecimal balance = ClientPlayerStats.getResourceBalance(resource);
        return balance.signum() >= 0 && balance.compareTo(BigDecimal.valueOf(cost)) >= 0;
    }

    private void overlayWhiteCtrlHints(GuiLayout layout) {
        net.minecraft.client.gui.FontRenderer font = GuiFontRenderer.get(Minecraft.getMinecraft());
        String[] labels = {"Пробудить", "+ CTRL Поделиться в чате", "+ CTRL Ослабить"};
        int pad = layout.scaled(8, 4), x = layout.runePanelX + pad, y = layout.runePanelY + layout.runePanelHeight - 17;
        int availableWidth = layout.runePanelWidth - pad * 2, totalWidth = 0;
        for (String label : labels) totalWidth += 13 + font.getStringWidth(label); totalWidth += 20;
        float scale = Math.min(1.0F, availableWidth / (float) Math.max(1, totalWidth));
        GlStateManager.pushMatrix(); GlStateManager.translate(layout.canvasX, layout.canvasY, 0.0F); GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1.0F);
        GlStateManager.translate(x, y, 0.0F); GlStateManager.scale(scale, scale, 1.0F);
        try { int offset = 0; for (String label : labels) { if (label.startsWith("+ CTRL ")) font.drawString("+ CTRL", offset + 13, 0, TEXT_COLOR, false); offset += 13 + font.getStringWidth(label) + 10; } }
        finally { GlStateManager.popMatrix(); }
    }

    private int awakeningRank() {
        int minimum = RuneInventory.ADVANCED_RANK;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) { int rank = ClientRuneRanks.rankOf(rune); if (rank <= 0) return 0; minimum = Math.min(minimum, rank); }
        return minimum >= RuneInventory.ADVANCED_RANK ? 0 : minimum;
    }

    private void drawFinalStatTooltip(CardsGuiScreen screen, GuiLayout layout, int mouseX, int mouseY) {
        if (ClientPlayerStats.getActiveRuneStatusId() != RuneStatusRegistry.STATUS_POISON) return;
        int canvasX = layout.canvasMouseX(mouseX), canvasY = layout.canvasMouseY(mouseY);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = ClientRuneRanks.rankOf(rune); if (rank <= 0 || !screen.runeMatchesSearch(rune)) continue;
            int[] pos = layout.runeSlotPosition(rune);
            if (canvasX < pos[0] || canvasX >= pos[0] + layout.runeSlotWidth || canvasY < pos[1] || canvasY >= pos[1] + layout.runeSlotHeight) continue;
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune); if (definition == null) return;
            List<String> lines = RuneTooltipContent.lines(definition, rank, ClientRuneEnhancements.levelOf(rune), ClientRuneEnhancements.multiplierOf(rune), targetedPowerForSlot(rune));
            if (lines.isEmpty()) return;
            RuneTooltipRenderer.draw(lines, rank, mouseX, mouseY, screen.width, screen.height, GuiFontRenderer.get(Minecraft.getMinecraft())); return;
        }
    }

    private double targetedPowerForSlot(int targetSlot) {
        String targetKey = targetKeyForSlot(targetSlot); if (targetKey.isEmpty()) return 0.0D;
        double total = 0.0D;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = ClientRuneRanks.rankOf(rune); if (rank <= 0) continue;
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune); if (definition == null) continue;
            double multiplier = ClientRuneEnhancements.multiplierOf(rune);
            for (RuneTargetedPower power : definition.targetedPowers) if (power != null && targetKey.equals(power.targetKey)) total += Math.max(0.0D, power.resolve(rank, multiplier));
        }
        return Double.isFinite(total) ? total : 0.0D;
    }

    private static String targetKeyForSlot(int slot) { if (slot >= 0 && slot <= 3) return "killer_dagger"; if (slot >= 13 && slot <= 16) return "snake_fang"; return ""; }
    private static int withAlpha(int color, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF); }
}
