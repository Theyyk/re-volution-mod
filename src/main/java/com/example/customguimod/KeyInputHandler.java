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

    private boolean cardsWasPressed = false;

    public static void register() {
        keyOpenCards = new KeyBinding("key.customguimod.cards", Keyboard.KEY_C, "key.categories.customguimod");
        ClientRegistry.registerKeyBinding(keyOpenCards);
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
    }
}
