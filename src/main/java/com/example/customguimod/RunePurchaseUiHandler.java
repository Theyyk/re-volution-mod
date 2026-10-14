package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.util.List;

@SideOnly(Side.CLIENT)
public class RunePurchaseUiHandler {
    private static final int PURCHASE_OR_AWAKENING_BUTTON_ID = 0;
    private static final int ENHANCE_BUTTON_ID = 2;
    private static final int ENHANCE_AMOUNT_BUTTON_ID = 3;
    private static final int STATUS_BUTTON_GAP = 2;

    private GuiScreen trackedScreen;
    private List<GuiButton> trackedButtons;
    private int selectedEnhancementRune = -1;
    private int enhancementAmount = 1;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        trackedScreen = event.getGui();
        trackedButtons = event.getButtonList();
        selectedEnhancementRune = -1;
        enhancementAmount = 1;
        compactStatusButtons(event.getGui(), trackedButtons);
        installEnhancementAmountButton(event.getGui(), trackedButtons);
        applyState(event.getGui());
        applyPurchaseEconomy(event.getGui(), trackedButtons);
        applyAwakeningEconomy(event.getGui(), trackedButtons);
        applyEnhancementEconomy(event.getGui(), trackedButtons);
    }

    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        applyState(event.getGui());
        if (event.getGui() == trackedScreen) {
            compactStatusButtons(event.getGui(), trackedButtons);
            rememberHoveredEnhancementRune(event.getGui());
            applyPurchaseEconomy(event.getGui(), trackedButtons);
            applyAwakeningEconomy(event.getGui(), trackedButtons);
            applyEnhancementEconomy(event.getGui(), trackedButtons);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.getGui() instanceof CardsGuiScreen)) return;
        GuiButton button = event.getButton();
        if (button == null) return;

        if (button.id == ENHANCE_AMOUNT_BUTTON_ID) {
            enhancementAmount = enhancementAmount == 1 ? 5
                    : enhancementAmount == 5 ? 10
                    : enhancementAmount == 10 ? 100
                    : 1;
            applyEnhancementEconomy(event.getGui(), event.getButtonList());
            return;
        }

        if (button.id == ENHANCE_BUTTON_ID && selectedEnhancementRune >= 0) {
            NetworkHandler.INSTANCE.sendToServer(new PingPacket(
                    "enhance_rune:" + selectedEnhancementRune + ":" + enhancementAmount));
        }
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Post event) {
        if (!(event.getGui() instanceof CardsGuiScreen)) return;
        if (!RuneAwakeningInput.isAwakeningButton(Mouse.getEventButton())
                || !Mouse.getEventButtonState()) return;

        CardsGuiScreen screen = (CardsGuiScreen) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * screen.width / Math.max(1, mc.displayWidth);
        int mouseY = screen.height - Mouse.getEventY() * screen.height / Math.max(1, mc.displayHeight) - 1;
        int rune = screen.awakeningRuneAt(mouseX, mouseY);
        if (rune >= 0) {
            NetworkHandler.INSTANCE.sendToServer(new PingPacket("upgrade_rune:" + rune));
        }
    }

    private void compactStatusButtons(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null) return;
        int nextX = -1;
        for (GuiButton button : buttons) {
            if (!(button instanceof GuiControls.ElementButton)) continue;
            if (nextX < 0) nextX = button.x;
            button.x = nextX;
            nextX += button.width + STATUS_BUTTON_GAP;
        }
    }

    private void applyState(GuiScreen gui) {
        if (gui instanceof CardsGuiScreen) {
            ((CardsGuiScreen) gui).updatePurchaseAvailability();
        }
    }

    private void installEnhancementAmountButton(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null) return;
        for (GuiButton button : buttons) {
            if (button.id == ENHANCE_AMOUNT_BUTTON_ID) return;
        }

        GuiButton enhance = findButton(buttons, ENHANCE_BUTTON_ID);
        GuiButton purchaseAmount = findButton(buttons, GuiControls.AMOUNT_BUTTON_START_ID);
        if (enhance == null || purchaseAmount == null) return;

        buttons.add(new GuiControls.PanelButton(
                ENHANCE_AMOUNT_BUTTON_ID,
                enhance.x,
                purchaseAmount.y,
                purchaseAmount.width,
                purchaseAmount.height,
                "x" + enhancementAmount
        ));
    }

    private void rememberHoveredEnhancementRune(GuiScreen gui) {
        if (!(gui instanceof CardsGuiScreen) || !usesStatusPurchaseEconomy()) return;
        CardsGuiScreen screen = (CardsGuiScreen) gui;
        int hovered = hoveredOwnedRune(screen);
        if (hovered >= 0) selectedEnhancementRune = hovered;
        if (selectedEnhancementRune >= 0 && ClientRuneRanks.rankOf(selectedEnhancementRune) <= 0) {
            selectedEnhancementRune = -1;
        }
    }

    private int hoveredOwnedRune(CardsGuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getX() * screen.width / Math.max(1, mc.displayWidth);
        int mouseY = screen.height - Mouse.getY() * screen.height / Math.max(1, mc.displayHeight) - 1;

        GuiLayout layout = new GuiLayout(RuneInventory.BOOST_TYPE_COUNT);
        layout.update(screen.width, screen.height);
        int x = layout.canvasMouseX(mouseX);
        int y = layout.canvasMouseY(mouseY);

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (ClientRuneRanks.rankOf(rune) <= 0 || !screen.runeMatchesSearch(rune)) continue;
            int[] pos = layout.runeSlotPosition(rune);
            if (x >= pos[0] && x < pos[0] + layout.runeSlotWidth
                    && y >= pos[1] && y < pos[1] + layout.runeSlotHeight) {
                return rune;
            }
        }
        return -1;
    }

    private void applyEnhancementEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null) return;

        GuiButton enhance = findButton(buttons, ENHANCE_BUTTON_ID);
        GuiButton amount = findButton(buttons, ENHANCE_AMOUNT_BUTTON_ID);
        if (amount != null) {
            amount.displayString = "x" + enhancementAmount;
            amount.visible = usesStatusPurchaseEconomy();
            amount.enabled = amount.visible && selectedEnhancementRune >= 0;
        }
        if (enhance == null) return;

        if (!usesStatusPurchaseEconomy()) {
            enhance.enabled = false;
            enhance.displayString = "Усиление:\nНедоступно";
            return;
        }
        if (selectedEnhancementRune < 0) {
            enhance.enabled = false;
            enhance.displayString = "Усилить руну:\nНаведите на руну";
            return;
        }

        int level = ClientRuneEnhancements.levelOf(selectedEnhancementRune);
        int cost = ClientRuneEnhancements.costOf(selectedEnhancementRune, enhancementAmount);
        String resource = ClientRuneEnhancements.getResource();
        String resourceName = resource == null || resource.isEmpty() ? "" : priceResourceName(resource);
        int balance = ClientPlayerStats.getResourceBalance(resource);

        if (cost <= 0 || resourceName.isEmpty()) {
            enhance.enabled = false;
            enhance.displayString = "Усиление +" + level + ":\nНе настроено";
            return;
        }

        enhance.displayString = "Усиление +" + level + ":\n" + cost + " " + resourceName;
        enhance.enabled = balance >= cost;
    }

    private void applyPurchaseEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null || !usesStatusPurchaseEconomy()) return;

        int unitCost = Math.max(1, ClientPlayerStats.getPurchaseUnitCost());
        long total = (long) unitCost * currentBuyAmount(buttons);
        if (total > Integer.MAX_VALUE) total = Integer.MAX_VALUE;

        String resource = ClientPlayerStats.getPurchaseResource();
        String resourceName = priceResourceName(resource);

        for (GuiButton button : buttons) {
            if (button.id == PURCHASE_OR_AWAKENING_BUTTON_ID && button.enabled) {
                button.displayString = "Купить руну:\n" + total + " " + resourceName;
                return;
            }
        }
    }

    private void applyAwakeningEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null || !usesStatusPurchaseEconomy()) return;

        GuiButton awakeningButton = null;
        for (GuiButton button : buttons) {
            if (button.id == PURCHASE_OR_AWAKENING_BUTTON_ID
                    && "Пробудить руну".equals(button.displayString)) {
                awakeningButton = button;
                break;
            }
        }
        if (awakeningButton == null) return;

        String resource = ClientPlayerStats.getAwakeningResource();
        String resourceName = resource == null || resource.isEmpty()
                ? "" : priceResourceName(resource);

        int normalCost = ClientPlayerStats.getAwakeningNormalCost();
        int improvedCost = ClientPlayerStats.getAwakeningImprovedCost();
        int displayedCost = resolveHoveredAwakeningCost((CardsGuiScreen) gui, normalCost, improvedCost);

        if (displayedCost > 0 && !resourceName.isEmpty()) {
            awakeningButton.displayString = "Пробудить руну:\n" + displayedCost + " " + resourceName;
            return;
        }

        if (normalCost > 0 && improvedCost > 0 && !resourceName.isEmpty()) {
            int min = Math.min(normalCost, improvedCost);
            int max = Math.max(normalCost, improvedCost);
            awakeningButton.displayString = min == max
                    ? "Пробудить руну:\n" + min + " " + resourceName
                    : "Пробудить руну:\n" + min + "–" + max + " " + resourceName;
            return;
        }

        awakeningButton.displayString = "Пробудить руну:\nБесплатно";
    }

    private int resolveHoveredAwakeningCost(CardsGuiScreen screen, int normalCost, int improvedCost) {
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getX() * screen.width / Math.max(1, mc.displayWidth);
        int mouseY = screen.height - Mouse.getY() * screen.height / Math.max(1, mc.displayHeight) - 1;
        int rune = screen.awakeningRuneAt(mouseX, mouseY);
        if (rune < 0) return 0;

        int rank = ClientRuneRanks.rankOf(rune);
        if (rank >= RuneInventory.IMPROVED_RANK) return improvedCost;
        if (rank >= RuneInventory.NORMAL_RANK) return normalCost;
        return 0;
    }

    private boolean usesStatusPurchaseEconomy() {
        return RuneStatusRegistry.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId());
    }

    private String priceResourceName(String resource) {
        String normalized = ResourceManager.normalize(resource);
        if ("coins".equals(normalized)) return "монет";
        if ("crystals".equals(normalized)) return "кристаллов";
        if (ResourceManager.RUNE_DUST.equals(normalized)) return "пыли рун";
        if (ResourceManager.RUNE_SHARDS.equals(normalized)) return "осколков рун";
        if (ResourceManager.RUNE_CORES.equals(normalized)) return "ядер рун";
        return ResourceManager.getDisplayName(resource).toLowerCase(java.util.Locale.ROOT);
    }

    private int currentBuyAmount(List<GuiButton> buttons) {
        for (GuiButton button : buttons) {
            if (button.id != GuiControls.AMOUNT_BUTTON_START_ID) continue;
            String label = button.displayString == null ? "" : button.displayString.trim();
            if (!label.startsWith("x")) return 1;
            try {
                int amount = Integer.parseInt(label.substring(1));
                return amount > 0 ? amount : 1;
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private GuiButton findButton(List<GuiButton> buttons, int id) {
        if (buttons == null) return null;
        for (GuiButton button : buttons) {
            if (button.id == id) return button;
        }
        return null;
    }
}
