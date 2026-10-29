package com.example.revolution.client.settings;

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
    public static KeyBinding keyOpenSettings;

    public static void register() {
        keyOpenSettings = new KeyBinding("key.revolution.settings", Keyboard.KEY_P, "key.categories.revolution");
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
