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

    private GuiScreen trackedScreen;
    private List<GuiButton> trackedButtons;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        trackedScreen = event.getGui();
        trackedButtons = event.getButtonList();
        applyState(event.getGui());
        applyPurchaseEconomy(event.getGui(), trackedButtons);
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        applyState(event.getGui());
        if (event.getGui() == trackedScreen) {
            applyPurchaseEconomy(event.getGui(), trackedButtons);
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
            NetworkHandler.INSTANCE.sendToServer(new PingPacket("awaken_rune:" + rune));
        }
    }

    private void applyState(GuiScreen gui) {
        if (gui instanceof CardsGuiScreen) {
            // The screen owns labels and IDs; legacy overrides conflict with the new controls.
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
            if (button.id == 0 && button.enabled) {
                button.displayString = "Купить руну:\n" + total + " " + resourceName;
                return;
            }
        }
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
        // Before deck state arrives the first status collection is the screen default.
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
