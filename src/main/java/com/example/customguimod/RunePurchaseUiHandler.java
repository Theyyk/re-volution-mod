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
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class RunePurchaseUiHandler {
    private static final int FIRST_STATUS_ELEMENT_BUTTON_ID = 100;
    private static final int PURCHASE_OR_AWAKENING_BUTTON_ID = 0;

    private GuiScreen trackedScreen;
    private List<GuiButton> trackedButtons;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        trackedScreen = event.getGui();
        trackedButtons = event.getButtonList();
        applyState(event.getGui());
        applyPurchaseEconomy(event.getGui(), trackedButtons);
        applyAwakeningEconomy(event.getGui(), trackedButtons);
    }

    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        applyState(event.getGui());
        if (event.getGui() == trackedScreen) {
            applyPurchaseEconomy(event.getGui(), trackedButtons);
            applyAwakeningEconomy(event.getGui(), trackedButtons);
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
            // Compatibility transport name only. Awakening and upgrade are separate gameplay mechanics;
            // the actual upgrade mechanic is not implemented yet.
            NetworkHandler.INSTANCE.sendToServer(new PingPacket("upgrade_rune:" + rune));
        }
    }

    private void applyState(GuiScreen gui) {
        if (gui instanceof CardsGuiScreen) {
            ((CardsGuiScreen) gui).updatePurchaseAvailability();
        }
    }

    private void applyPurchaseEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null || !usesStatusPurchaseEconomy(buttons)) return;

        int unitCost = Math.max(1, ClientPlayerStats.getPurchaseUnitCost());
        long total = (long) unitCost * currentBuyAmount(buttons);
        if (total > Integer.MAX_VALUE) total = Integer.MAX_VALUE;

        String resource = ClientPlayerStats.getPurchaseResource();
        String resourceName = ResourceManager.getDisplayName(resource).toLowerCase(Locale.ROOT);

        for (GuiButton button : buttons) {
            if (button.id == PURCHASE_OR_AWAKENING_BUTTON_ID && button.enabled) {
                button.displayString = "Купить руну:\n" + total + " " + resourceName;
                return;
            }
        }
    }

    private void applyAwakeningEconomy(GuiScreen gui, List<GuiButton> buttons) {
        if (!(gui instanceof CardsGuiScreen) || buttons == null || !usesStatusPurchaseEconomy(buttons)) return;

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
                ? "" : ResourceManager.getDisplayName(resource).toLowerCase(Locale.ROOT);

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

    private boolean usesStatusPurchaseEconomy(List<GuiButton> buttons) {
        boolean foundElementButton = false;
        for (GuiButton button : buttons) {
            if (!(button instanceof GuiControls.ElementButton)) continue;
            foundElementButton = true;
            GuiControls.ElementButton element = (GuiControls.ElementButton) button;
            if (element.isActiveElement()) {
                return button.id == FIRST_STATUS_ELEMENT_BUTTON_ID;
            }
        }
        return !foundElementButton;
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
}
