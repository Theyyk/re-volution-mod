package com.example.revolution.client.mechanic;

import com.example.revolution.client.RunesGuiScreen;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Bootstrap-accessible Unlock tab injected into the Standard Rune screen. */
public final class MechanicUnlockTabHandler {
    public static final int BUTTON_ID = 201;
    private GuiScreen screen;
    private GuiButton button;

    @SubscribeEvent public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        screen = event.getGui();
        button = new GuiButton(BUTTON_ID, 8, 30, 100, 20, label());
        event.getButtonList().add(button);
    }

    @SubscribeEvent public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != BUTTON_ID) return;
        event.setCanceled(true);
        Minecraft.getMinecraft().displayGuiScreen(new MechanicUnlockGuiScreen());
    }

    @SubscribeEvent public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() != screen || button == null) return;
        button.displayString = label();
    }

    private static String label() { return ClientMechanicUnlocks.readyHighlight() ? "§eUnlocks *" : "Unlocks"; }
}
