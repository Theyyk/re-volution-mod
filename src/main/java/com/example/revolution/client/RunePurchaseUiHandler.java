package com.example.revolution.client;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PingPacket;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class RunePurchaseUiHandler {
    private static final int PURCHASE_OR_AWAKENING_BUTTON_ID = 0;
    private static final int ENHANCE_BUTTON_ID = 2;
    private static final int ENHANCE_AMOUNT_BUTTON_ID = 3;
    private static final int STATUS_BUTTON_GAP = 2;
    private static final int AMOUNT_MIN_WIDTH = 42;

    private GuiScreen trackedScreen;
    private List<GuiButton> trackedButtons;
    private int enhancementAmount = 1;

    @SubscribeEvent public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        trackedScreen = event.getGui(); trackedButtons = event.getButtonList(); enhancementAmount = 1;
        compactStatusButtons(event.getGui(), trackedButtons); installEnhancementAmountButton(event.getGui(), trackedButtons);
        applyState(event.getGui()); applyPurchaseEconomy(event.getGui(), trackedButtons); applyAwakeningEconomy(event.getGui(), trackedButtons); applyEnhancementEconomy(event.getGui(), trackedButtons);
    }

    @SubscribeEvent public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        applyState(event.getGui());
        if (event.getGui() == trackedScreen) {
            installEnhancementAmountButton(event.getGui(), trackedButtons); compactStatusButtons(event.getGui(), trackedButtons);
            applyPurchaseEconomy(event.getGui(), trackedButtons); applyAwakeningEconomy(event.getGui(), trackedButtons); applyEnhancementEconomy(event.getGui(), trackedButtons);
        }
    }

    @SubscribeEvent public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        GuiButton button = event.getButton(); if (button == null) return;
        if (button.id == ENHANCE_AMOUNT_BUTTON_ID) {
            enhancementAmount = enhancementAmount == 1 ? 5 : enhancementAmount == 5 ? 10 : enhancementAmount == 10 ? 100 : 1;
            applyEnhancementEconomy(event.getGui(), event.getButtonList()); return;
        }
        if (button.id == ENHANCE_BUTTON_ID && usesStatusPurchaseEconomy()) {
            if (!enhancementCostRange(enhancementAmount).valid) return;
            NetworkHandler.INSTANCE.sendToServer(new PingPacket("enhance_rune:0:" + enhancementAmount));
        }
    }

    @SubscribeEvent public void onMouseInput(GuiScreenEvent.MouseInputEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || !Mouse.getEventButtonState()) return;
        int button = Mouse.getEventButton();
        if (!RuneAwakeningInput.isAwakeningButton(button) && !RuneRankRevertInput.isRevertButton(button)) return;

        RunesGuiScreen screen = (RunesGuiScreen) event.getGui(); Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * screen.width / Math.max(1, mc.displayWidth);
        int mouseY = screen.height - Mouse.getEventY() * screen.height / Math.max(1, mc.displayHeight) - 1;

        if (RuneRankRevertInput.isRevertButton(button)) {
            GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
            layout.update(screen.width, screen.height);
            int rune = RuneRankRevertInput.runeAt(layout, mouseX, mouseY,
                    screen::runeMatchesSearch, ClientRuneRanks::rankOf);
            if (rune >= 0) NetworkHandler.INSTANCE.sendToServer(new PingPacket("revert_rune:" + rune));
            return;
        }

        int rune = screen.awakeningRuneAt(mouseX, mouseY);
        if (rune >= 0) NetworkHandler.INSTANCE.sendToServer(new PingPacket("upgrade_rune:" + rune));
    }

    private void compactStatusButtons(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof RunesGuiScreen) || buttons == null) return;
        int nextX = -1;
        for (GuiButton button : buttons) { if (!(button instanceof GuiControls.ElementButton)) continue; if (nextX < 0) nextX = button.x; button.x = nextX; nextX += button.width + STATUS_BUTTON_GAP; }
    }

    private void applyState(GuiScreen gui) { if (gui instanceof RunesGuiScreen) ((RunesGuiScreen) gui).updatePurchaseAvailability(); }

    private void installEnhancementAmountButton(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof RunesGuiScreen) || buttons == null) return;
        GuiButton enhance = findButton(buttons, ENHANCE_BUTTON_ID), purchaseAmount = findButton(buttons, GuiControls.AMOUNT_BUTTON_START_ID);
        if (enhance == null || purchaseAmount == null) return;
        int amountWidth = Math.max(purchaseAmount.width, AMOUNT_MIN_WIDTH); purchaseAmount.width = amountWidth;
        GuiButton amount = findButton(buttons, ENHANCE_AMOUNT_BUTTON_ID);
        if (amount == null) { amount = new GuiControls.PanelButton(ENHANCE_AMOUNT_BUTTON_ID, enhance.x, enhance.y + enhance.height + 6, amountWidth, purchaseAmount.height, "x" + enhancementAmount); buttons.add(amount); }
        else { amount.x = enhance.x; amount.y = enhance.y + enhance.height + 6; amount.width = amountWidth; }
    }

    private void applyEnhancementEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof RunesGuiScreen) || buttons == null) return;
        boolean statusEconomy = usesStatusPurchaseEconomy();
        GuiButton enhance = findButton(buttons, ENHANCE_BUTTON_ID), amount = findButton(buttons, ENHANCE_AMOUNT_BUTTON_ID);
        if (amount != null) { amount.displayString = "x" + enhancementAmount; amount.visible = statusEconomy; amount.enabled = amount.visible; }
        if (enhance == null) return;
        if (!statusEconomy) { enhance.enabled = false; enhance.displayString = "Усиление:\nНедоступно"; return; }
        CostRange costs = enhancementCostRange(enhancementAmount); String resource = ClientRuneEnhancements.getResource();
        String resourceName = resource == null || resource.isEmpty() ? "" : priceResourceName(resource);
        BigDecimal balance = ClientPlayerStats.getResourceBalance(resource);
        if (!costs.valid) { enhance.enabled = false; enhance.displayString = "Случайное усиление:\nНе настроено"; return; }
        if (resourceName.isEmpty()) { enhance.enabled = false; enhance.displayString = "Случайное усиление:\nНе настроено"; return; }
        String costText = costs.minimum.compareTo(costs.maximum) == 0
                ? LargeNumbers.format(costs.maximum)
                : LargeNumbers.format(costs.minimum) + "–" + LargeNumbers.format(costs.maximum);
        enhance.displayString = "Случайное усиление:\n" + costText + " " + resourceName;
        enhance.enabled = balance.compareTo(costs.maximum) >= 0;
    }

    private CostRange enhancementCostRange(int amount) {
        BigDecimal minimum = null, maximum = null;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            BigDecimal cost = ClientRuneEnhancements.costOf(rune, amount);
            if (cost == null || cost.signum() <= 0) return CostRange.invalid();
            minimum = minimum == null || cost.compareTo(minimum) < 0 ? cost : minimum;
            maximum = maximum == null || cost.compareTo(maximum) > 0 ? cost : maximum;
        }
        return minimum == null ? CostRange.invalid() : new CostRange(true, minimum, maximum);
    }

    private void applyPurchaseEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof RunesGuiScreen) || buttons == null || !usesStatusPurchaseEconomy()) return;
        int unitCost = Math.max(1, ClientPlayerStats.getPurchaseUnitCost()); long total = (long) unitCost * currentBuyAmount(buttons);
        if (total > Integer.MAX_VALUE) total = Integer.MAX_VALUE;
        String resource = ClientPlayerStats.getPurchaseResource(), resourceName = priceResourceName(resource);
        for (GuiButton button : buttons) if (button.id == PURCHASE_OR_AWAKENING_BUTTON_ID && button.enabled) { button.displayString = "Купить руну:\n" + total + " " + resourceName; return; }
    }

    private void applyAwakeningEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof RunesGuiScreen) || buttons == null || !usesStatusPurchaseEconomy()) return;
        GuiButton awakeningButton = null;
        for (GuiButton button : buttons) if (button.id == PURCHASE_OR_AWAKENING_BUTTON_ID && "Пробудить руну".equals(button.displayString)) { awakeningButton = button; break; }
        if (awakeningButton == null) return;
        String resource = ClientPlayerStats.getAwakeningResource(); String resourceName = resource == null || resource.isEmpty() ? "" : priceResourceName(resource);
        int normalCost = ClientPlayerStats.getAwakeningNormalCost(), improvedCost = ClientPlayerStats.getAwakeningImprovedCost();
        int displayedCost = resolveHoveredAwakeningCost((RunesGuiScreen) gui, normalCost, improvedCost);
        if (displayedCost > 0 && !resourceName.isEmpty()) { awakeningButton.displayString = "Пробудить руну:\n" + displayedCost + " " + resourceName; return; }
        if (normalCost > 0 && improvedCost > 0 && !resourceName.isEmpty()) {
            int min = Math.min(normalCost, improvedCost), max = Math.max(normalCost, improvedCost);
            awakeningButton.displayString = min == max ? "Пробудить руну:\n" + min + " " + resourceName : "Пробудить руну:\n" + min + "–" + max + " " + resourceName; return;
        }
        awakeningButton.displayString = "Пробудить руну:\nБесплатно";
    }

    private int resolveHoveredAwakeningCost(RunesGuiScreen screen, int normalCost, int improvedCost) {
        Minecraft mc = Minecraft.getMinecraft(); int mouseX = Mouse.getX() * screen.width / Math.max(1, mc.displayWidth);
        int mouseY = screen.height - Mouse.getY() * screen.height / Math.max(1, mc.displayHeight) - 1;
        int rune = screen.awakeningRuneAt(mouseX, mouseY); if (rune < 0) return 0;
        int rank = ClientRuneRanks.rankOf(rune); if (rank >= RuneInventory.IMPROVED_RANK) return improvedCost; if (rank >= RuneInventory.NORMAL_RANK) return normalCost; return 0;
    }

    private boolean usesStatusPurchaseEconomy() { return RuneStatusConfig.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId()); }

    private String priceResourceName(String resource) {
        String normalized = ResourceDefinitions.normalize(resource);
        if ("coins".equals(normalized)) return "монет"; if ("crystals".equals(normalized)) return "кристаллов";
        if (ResourceDefinitions.RUNE_DUST.equals(normalized)) return "пыли рун"; if (ResourceDefinitions.RUNE_SHARDS.equals(normalized)) return "осколков рун";
        if (ResourceDefinitions.RUNE_CORES.equals(normalized)) return "ядер рун"; return ResourceDefinitions.getDisplayName(resource).toLowerCase(Locale.ROOT);
    }

    private int currentBuyAmount(List<GuiButton> buttons) {
        for (GuiButton button : buttons) {
            if (button.id != GuiControls.AMOUNT_BUTTON_START_ID) continue;
            String label = button.displayString == null ? "" : button.displayString.trim(); if (!label.startsWith("x")) return 1;
            try { int amount = Integer.parseInt(label.substring(1)); return amount > 0 ? amount : 1; } catch (NumberFormatException ignored) { return 1; }
        }
        return 1;
    }

    private GuiButton findButton(List<GuiButton> buttons, int id) { if (buttons == null) return null; for (GuiButton button : buttons) if (button.id == id) return button; return null; }

    private static final class CostRange {
        final boolean valid; final BigDecimal minimum; final BigDecimal maximum;
        CostRange(boolean valid, BigDecimal minimum, BigDecimal maximum) { this.valid = valid; this.minimum = minimum; this.maximum = maximum; }
        static CostRange invalid() { return new CostRange(false, BigDecimal.ZERO, BigDecimal.ZERO); }
    }
}
