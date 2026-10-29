package com.example.revolution.client.input;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.StageTravelMenuRequestPacket;
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

    public static KeyBinding keyOpenStageTravel;

    private boolean stageTravelWasPressed = false;

    public static void register() {
        keyOpenStageTravel = new KeyBinding("key.revolution.stage_travel", Keyboard.KEY_L, "key.categories.revolution");
        ClientRegistry.registerKeyBinding(keyOpenStageTravel);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        boolean stageTravelPressed = keyOpenStageTravel.isPressed();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (stageTravelPressed && !stageTravelWasPressed
                && minecraft.player != null && minecraft.getConnection() != null
                && minecraft.currentScreen == null) {
            NetworkHandler.INSTANCE.sendToServer(new StageTravelMenuRequestPacket());
        }
        stageTravelWasPressed = stageTravelPressed;
    }
}
