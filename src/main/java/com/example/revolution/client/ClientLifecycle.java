package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import com.example.revolution.client.hud.HealthBarRenderer;
import com.example.revolution.client.hud.NameHider;
import com.example.revolution.client.settings.SettingsKeyHandler;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.number.LargeNumbers;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientLifecycle {
    private ClientLifecycle() {}
    public static void preInit(File directory) {
        ServerLifecycle.configureClient(directory, RevolutionMod.logger);
        LargeNumbers.initDisplaySettings(directory);
        ClientNetworkRegistration.register();
    }
    public static void init() {

        KeyInputHandler.register();
        AdminPanelKeyHandler.register();
        SettingsKeyHandler.register();
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new AdminPanelKeyHandler());
        MinecraftForge.EVENT_BUS.register(new SettingsKeyHandler());
        MinecraftForge.EVENT_BUS.register(new ClientPacketHandler());
        MinecraftForge.EVENT_BUS.register(new HealthBarRenderer());
        MinecraftForge.EVENT_BUS.register(new NameHider());
        MinecraftForge.EVENT_BUS.register(new EnhancementClickGuard());
        MinecraftForge.EVENT_BUS.register(new RunePurchaseUiHandler());
        MinecraftForge.EVENT_BUS.register(new RuneUxUiHandler());
        MinecraftForge.EVENT_BUS.register(new RuneStatusChatFilter());
        RuneTexturePreloader.register();

        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
        GuiFontRenderer.get(minecraft);
        GuiFontRenderer.heading(minecraft);

        RevolutionMod.logger.info("RE:volution Mod client init done");
    }
}
