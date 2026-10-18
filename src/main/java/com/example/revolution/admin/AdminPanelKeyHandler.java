package com.example.revolution.admin;

import com.example.revolution.AdminPanelRequestPacket;
import com.example.revolution.network.NetworkHandler;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class AdminPanelKeyHandler {
    private boolean wasPressed;

    public static void register() {
        // Intentionally not registered as a Minecraft KeyBinding: the admin shortcut
        // must not be exposed in the public Controls menu.
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        boolean pressed = Keyboard.isKeyDown(Keyboard.KEY_F8);
        if (pressed && !wasPressed) {
            NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket("open"));
        }
        wasPressed = pressed;
    }
}
