package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import com.example.revolution.client.hud.HealthBarRenderer;
import com.example.revolution.client.hud.NameHider;
import com.example.revolution.client.input.EnhancementClickGuard;
import com.example.revolution.client.input.KeyInputHandler;
import com.example.revolution.client.settings.SettingsKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

/** Client composition boundary used by the physical-side lifecycle adapter. */
public final class ClientBootstrap {
    private ClientBootstrap() {}

    public static void init() {
        KeyInputHandler.register();
        SettingsKeyHandler.register();
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new SettingsKeyHandler());
        MinecraftForge.EVENT_BUS.register(new ClientPacketHandler());
        MinecraftForge.EVENT_BUS.register(new HealthBarRenderer());
        MinecraftForge.EVENT_BUS.register(new NameHider());
        MinecraftForge.EVENT_BUS.register(new EnhancementClickGuard());
        MinecraftForge.EVENT_BUS.register(new RunePurchaseUiHandler());
        MinecraftForge.EVENT_BUS.register(new RuneUxUiHandler());
        MinecraftForge.EVENT_BUS.register(new RuneStatusChatFilter());
        RuneTexturePreloader.register();

        Minecraft minecraft = Minecraft.getMinecraft();
        GuiFontRenderer.get(minecraft);
        GuiFontRenderer.heading(minecraft);

        RevolutionMod.logger.info("RE:volution Mod client init done");
    }
}
