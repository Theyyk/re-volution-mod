package com.example.revolution.lifecycle;

import com.example.revolution.combat.DamageBonusHandler;
import com.example.revolution.commands.AdminPanelCommand;
import com.example.revolution.commands.CustomMobCommand;
import com.example.revolution.commands.CustomResourceCommand;
import com.example.revolution.commands.CustomRunesCommand;
import com.example.revolution.commands.CustomStatusCommand;
import com.example.revolution.commands.RealmCommand;
import com.example.revolution.mob.event.ManagedMobEvents;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RateLimitedPingPacketHandler;
import com.example.revolution.player.event.PlayerJoinHandler;
import com.example.revolution.realm.event.RealmEntityLoadHandler;
import com.example.revolution.realm.event.RealmLoginHandler;
import com.example.revolution.realm.world.RealmStartupSync;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

/** Forge lifecycle boundary; application services must never call this locator. */
public final class ServerLifecycle {
    private static ServerServices current;
    private static File configDirectory;
    private static Logger logger;
    private static boolean dedicatedServer;
    private static RealmStartupSync realmSave;
    private static final List<Object> listeners = new ArrayList<>();
    private ServerLifecycle() {}

    public static void preInit(File directory, Logger log) {
        configure(directory, log, true);
        NetworkHandler.registerServer();
        initialize();
    }

    /** Stores context on the physical client without starting server storage or listeners. */
    public static void configureClient(File directory, Logger log) { configure(directory, log, false); }

    private static void configure(File directory, Logger log, boolean dedicated) {
        configDirectory = directory;
        logger = log;
        dedicatedServer = dedicated;
    }

    private static void initialize() {
        if (current != null) return;
        current = new ServerServices(logger, dedicatedServer);
        // Preserve the old integrated-server boundary: commands/resources exist, but the
        // core player/mob repository and dedicated gameplay listeners are not activated.
        if (!dedicatedServer) return;
        current.connect();
        current.adminAccess.init(configDirectory);
        listeners.add(new PlayerJoinHandler(current.sessions));
        listeners.add(new RealmLoginHandler(current.realms));
        listeners.add(new RealmEntityLoadHandler());
        listeners.add(new ManagedMobEvents(current.mobs));
        listeners.add(new DamageBonusHandler(current.mobs, current.combat, current.playerStats));
        for (Object listener : listeners) MinecraftForge.EVENT_BUS.register(listener);
    }

    public static ServerServices services() {
        if (current == null) throw new IllegalStateException("Server services are not initialized");
        return current;
    }

    public static void aboutToStart(FMLServerAboutToStartEvent event) {
        if (!dedicatedServer || !event.getServer().isDedicatedServer()) return;
        if (realmSave != null || DimensionManager.getWorlds().length != 0) {
            throw new IllegalStateException("Realm startup sync requires all Forge worlds to be unloaded");
        }
        MinecraftServer server = event.getServer();
        // getFile resolves --world/--universe without creating a SaveHandler/session.lock.
        File world = server.getActiveAnvilConverter().getFile(server.getFolderName(), "");
        try {
            realmSave = RealmStartupSync.prepare(world.toPath());
            logger.info("Realm startup sync complete: DIM201 -> DIM202/DIM203; dimension 0 excluded.");
        } catch (IOException e) {
            throw new IllegalStateException("Realm startup sync failed; refusing to load worlds", e);
        }
    }

    public static void starting(FMLServerStartingEvent event) {
        // Physical-client context is supplied by ClientLifecycle; command availability is preserved.
        if (logger == null) return;
        initialize();
        event.registerServerCommand(new CustomMobCommand(current.mobAdmin));
        event.registerServerCommand(new CustomRunesCommand(current.runeAdmin));
        event.registerServerCommand(new CustomStatusCommand(current.collections));
        event.registerServerCommand(new CustomResourceCommand(current.resources));
        event.registerServerCommand(new AdminPanelCommand(current.adminAccess));
        event.registerServerCommand(new RealmCommand(current.realms));
        current.resources.purgeDeprecatedLightnings();
    }

    public static void started() {
        if (current == null) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            logger.error("MinecraftServer is unavailable during FMLServerStartedEvent; managed mobs were not loaded.");
            return;
        }
        current.realms.initialize(server);
        current.mobs.loadFromDatabase(server);
        if (dedicatedServer) {
            try {
                current.startAdminApi(server, configDirectory);
            } catch (IOException | RuntimeException e) {
                logger.error("RE:volution Admin API failed closed and was not started.", e);
            }
        }
    }

    public static void stopped() {
        for (Object listener : listeners) MinecraftForge.EVENT_BUS.unregister(listener);
        listeners.clear();
        ServerServices stopping = current;
        current = null;
        RateLimitedPingPacketHandler.reset();
        try {
            if (stopping != null) stopping.close();
        } finally {
            if (realmSave != null) {
                try { realmSave.close(); }
                catch (IOException e) { logger.error("Could not release Realm save lock", e); }
                finally { realmSave = null; }
            }
        }
    }
}
