package com.example.customguimod;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class AdminPanelKeyHandler {
    private static KeyBinding keyOpenAdmin;

    public static void register() {
        keyOpenAdmin = new KeyBinding("key.customguimod.admin", Keyboard.KEY_F8, "key.categories.customguimod");
        ClientRegistry.registerKeyBinding(keyOpenAdmin);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || keyOpenAdmin == null) return;
        if (keyOpenAdmin.isPressed()) {
            NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket("open"));
        }
    }
}
