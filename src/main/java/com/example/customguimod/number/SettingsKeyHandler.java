package com.example.customguimod.number;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class SettingsKeyHandler {
    private static KeyBinding keyOpenSettings;

    public static void register() {
        keyOpenSettings = new KeyBinding("key.customguimod.settings", Keyboard.KEY_O, "key.categories.customguimod");
        ClientRegistry.registerKeyBinding(keyOpenSettings);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || keyOpenSettings == null) return;
        if (!keyOpenSettings.isPressed()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen == null) {
            minecraft.displayGuiScreen(new SettingsScreen());
        }
    }
}
