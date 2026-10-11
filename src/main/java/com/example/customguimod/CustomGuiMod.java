package com.example.customguimod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(modid = CustomGuiMod.MODID, version = CustomGuiMod.VERSION)
public class CustomGuiMod {

    public static final String MODID = "customguimod";
    public static final String VERSION = "1.1.0";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        if (event.getSide() == Side.SERVER) {
            NetworkHandler.registerServer();
            MongoManager.connect();
            AdminPanelAccess.init(event.getModConfigurationDirectory());
            MinecraftForge.EVENT_BUS.register(new PlayerJoinHandler());
            MinecraftForge.EVENT_BUS.register(new MobManager());
            MinecraftForge.EVENT_BUS.register(new DamageBonusHandler());
        } else {
            NetworkHandler.registerClient();
        }

        logger.info("CustomGuiMod preInit done");
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        KeyInputHandler.register();
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new ClientPacketHandler());
        MinecraftForge.EVENT_BUS.register(new HealthBarRenderer());
        MinecraftForge.EVENT_BUS.register(new NameHider());
        MinecraftForge.EVENT_BUS.register(new PlayerStatsHudRenderer());
        MinecraftForge.EVENT_BUS.register(new RunePurchaseUiHandler());
        RuneTexturePreloader.register();
        logger.info("CustomGuiMod client init done");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CustomMobCommand());
        event.registerServerCommand(new CustomCardsCommand());
        event.registerServerCommand(new CustomDeckCommand());
        event.registerServerCommand(new CustomResourceCommand());
        event.registerServerCommand(new AdminPanelCommand());
        net.minecraft.world.World world = event.getServer().getWorld(0);
        MobManager.loadFromDatabase(world);
    }
}
