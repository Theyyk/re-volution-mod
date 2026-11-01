package com.example.revolution;

import com.example.revolution.lifecycle.CommonProxy;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.realm.world.RealmDimensions;
import java.util.List;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.Logger;

/** Forge bootstrap. Server composition and client initialization live behind side-specific boundaries. */
@Mod(modid = RevolutionMod.MODID, name = RevolutionMod.NAME, version = RevolutionMod.VERSION, acceptableRemoteVersions = "*")
public final class RevolutionMod {
    public static final String MODID = "revolution";
    public static final String NAME = "RE:volution Mod";
    public static final String VERSION = "2.12.0";
    public static Logger logger;

    @Mod.Instance(MODID)
    public static RevolutionMod instance;

    @SidedProxy(clientSide = "com.example.revolution.client.ClientProxy", serverSide = "com.example.revolution.lifecycle.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        ForgeChunkManager.setForcedChunkLoadingCallback(this, new ForgeChunkManager.LoadingCallback() {
            @Override public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
                if (tickets == null) return;
                // Location build tickets are temporary leases only. Never restore stale leases after a restart.
                for (ForgeChunkManager.Ticket ticket : tickets) {
                    if (ticket != null) ForgeChunkManager.releaseTicket(ticket);
                }
            }
        });
        RealmDimensions.register();
        proxy.preInit(event);
        logger.info("RE:volution Mod preInit done");
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
    @Mod.EventHandler public void serverAboutToStart(FMLServerAboutToStartEvent event) { ServerLifecycle.aboutToStart(event); }
    @Mod.EventHandler public void serverStarting(FMLServerStartingEvent event) { ServerLifecycle.starting(event); }
    @Mod.EventHandler public void serverStarted(FMLServerStartedEvent event) { ServerLifecycle.started(); }
    @Mod.EventHandler public void serverStopped(FMLServerStoppedEvent event) { ServerLifecycle.stopped(); }
}
