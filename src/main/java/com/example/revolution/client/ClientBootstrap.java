package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import com.example.revolution.client.hud.HealthBarRenderer;
import com.example.revolution.client.hud.NameHider;
import com.example.revolution.client.hud.NotificationFeedRenderer;
import com.example.revolution.client.hud.RealmHudRenderer;
import com.example.revolution.client.hud.RpgHudRenderer;
import com.example.revolution.client.input.ClientRpgSurvivalGuard;
import com.example.revolution.client.input.EnhancementClickGuard;
import com.example.revolution.client.input.InventoryMenuRedirectHandler;
import com.example.revolution.client.input.KeyInputHandler;
import com.example.revolution.client.mechanic.MechanicUnlockTabHandler;
import com.example.revolution.client.mob.ManagedMobVisualRenderer;
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
        MinecraftForge.EVENT_BUS.register(new ManagedMobVisualRenderer());
        MinecraftForge.EVENT_BUS.register(new HealthBarRenderer());
        MinecraftForge.EVENT_BUS.register(new NameHider());
        MinecraftForge.EVENT_BUS.register(new RealmHudRenderer());
        MinecraftForge.EVENT_BUS.register(new NotificationFeedRenderer());
        MinecraftForge.EVENT_BUS.register(new RpgHudRenderer());
        MinecraftForge.EVENT_BUS.register(new MobZoneOverlayRenderer());
        MinecraftForge.EVENT_BUS.register(new EnhancementClickGuard());
        MinecraftForge.EVENT_BUS.register(new InventoryMenuRedirectHandler());
        MinecraftForge.EVENT_BUS.register(new ClientRpgSurvivalGuard());
        MinecraftForge.EVENT_BUS.register(new RunePurchaseUiHandler());
        MinecraftForge.EVENT_BUS.register(new RuneUxUiHandler());
        MinecraftForge.EVENT_BUS.register(new PetTabUiHandler());
        MinecraftForge.EVENT_BUS.register(new AchievementTabUiHandler());
        MinecraftForge.EVENT_BUS.register(new UniqueRuneTabUiHandler());
        MinecraftForge.EVENT_BUS.register(new MechanicUnlockTabHandler());
        RuneTexturePreloader.register();

        Minecraft minecraft = Minecraft.getMinecraft();
        GuiFontRenderer.get(minecraft);
        GuiFontRenderer.heading(minecraft);
        RevolutionMod.logger.info("RE:volution Mod client init done");
    }
}
