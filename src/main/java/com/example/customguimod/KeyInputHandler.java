package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    public static KeyBinding keyOpenCards;
    public static KeyBinding keyOpenAdminPanel;

    private boolean cardsWasPressed = false;
    private boolean adminWasPressed = false;

    public static void register() {
        keyOpenCards = new KeyBinding("key.customguimod.cards", Keyboard.KEY_C, "key.categories.customguimod");
        keyOpenAdminPanel = new KeyBinding("key.customguimod.admin_panel", Keyboard.KEY_F8, "key.categories.customguimod");
        ClientRegistry.registerKeyBinding(keyOpenCards);
        ClientRegistry.registerKeyBinding(keyOpenAdminPanel);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        boolean cardsPressed = keyOpenCards.isPressed();
        if (cardsPressed && !cardsWasPressed) {
            Minecraft.getMinecraft().displayGuiScreen(new CardsGuiScreen());
            CustomGuiMod.logger.info("Открываю CardsGuiScreen");
        }
        cardsWasPressed = cardsPressed;

        boolean adminPressed = keyOpenAdminPanel.isPressed();
        if (adminPressed && !adminWasPressed) {
            NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket("open"));
        }
        adminWasPressed = adminPressed;
    }
}
