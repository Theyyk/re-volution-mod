package com.example.revolution.client.input;

import com.example.revolution.RevolutionMod;
import com.example.revolution.client.ClientUiBridge;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    public static KeyBinding keyOpenRunes;

    private boolean runesWasPressed = false;

    public static void register() {
        keyOpenRunes = new KeyBinding("key.revolution.cards", Keyboard.KEY_C, "key.categories.revolution");
        ClientRegistry.registerKeyBinding(keyOpenRunes);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ClientUiBridge.tickRunePreloader();

        boolean runesPressed = keyOpenRunes.isPressed();
        if (runesPressed && !runesWasPressed && !ClientUiBridge.isRuneOpenPending()) {
            ClientUiBridge.beginRuneOpen();
            RevolutionMod.logger.info("Предзагружаю RuneResourcesCardsGuiScreen");
        }
        runesWasPressed = runesPressed;
    }
}
