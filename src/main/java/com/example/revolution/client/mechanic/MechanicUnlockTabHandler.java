package com.example.revolution.client.mechanic;

import com.example.revolution.client.RunesGuiScreen;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Bootstrap-accessible Unlock tab injected into the Standard Rune screen. */
public final class MechanicUnlockTabHandler {
    public static final int BUTTON_ID = 201;
    @SubscribeEvent public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        event.getButtonList().add(new GuiButton(BUTTON_ID, 8, 30, 100, 20, label()));
    }
    @SubscribeEvent public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen) || event.getButton().id != BUTTON_ID) return;
        event.setCanceled(true);
        Minecraft.getMinecraft().displayGuiScreen(new MechanicUnlockGuiScreen());
    }
    @SubscribeEvent public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;
        for (GuiButton button : event.getGui().buttonList) if (button.id == BUTTON_ID) button.displayString = label();
    }
    private static String label() { return ClientMechanicUnlocks.readyHighlight() ? "§eUnlocks *" : "Unlocks"; }
}
