package com.example.revolution.lifecycle;

import com.example.revolution.admin.api.AdminApiServer;
import com.example.revolution.admin.service.AdminAccessService;
import com.example.revolution.admin.service.AdminOperationsService;
import com.example.revolution.admin.service.AdminQueryService;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.repository.StageTransitionPreferenceRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.example.revolution.location.service.MobZoneOverlayService;
import com.example.revolution.location.service.StageTransitionService;
import com.example.revolution.location.service.StageTransitionTriggerService;
import com.example.revolution.mob.repository.MobProgressionRepository;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.mob.service.MobAdminService;
import com.example.revolution.mob.service.MobProgressionService;
import com.example.revolution.mob.service.MobRewardService;
import com.example.revolution.persistence.FileAdminWhitelistRepository;
import com.example.revolution.persistence.MongoConnection;
import com.example.revolution.persistence.MongoLocationRepository;
import com.example.revolution.persistence.MongoMobProgressionRepository;
import com.example.revolution.persistence.MongoMobRepository;
import com.example.revolution.persistence.MongoPlayerRepository;
import com.example.revolution.persistence.MongoResourceRepository;
import com.example.revolution.persistence.MongoRuneEnhancementRepository;
import com.example.revolution.persistence.MongoStageTransitionPreferenceRepository;
import com.example.revolution.persistence.MongoStageTransitionTriggerRepository;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerCollectionService;
import com.example.revolution.player.service.PlayerSessionService;
import com.example.revolution.player.service.PlayerStatsService;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.realm.service.RealmService;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.service.RuneActionService;
import com.example.revolution.rune.service.RuneAdminService;
import com.example.revolution.rune.service.RuneAwakeningPaymentService;
import com.example.revolution.rune.service.RuneAwakeningService;
import com.example.revolution.rune.service.RuneCombatService;
import com.example.revolution.rune.service.RuneEnhancementService;
import com.example.revolution.rune.service.RuneMobEffectService;
import com.example.revolution.rune.service.RunePurchaseService;
import com.example.revolution.rune.service.RuneRewardBonusService;
import com.example.revolution.rune.service.RuneSyncService;
import com.example.revolution.rune.state.RuneCombatState;
import java.io.File;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** One object graph per server lifetime. Only adapters may retrieve it from ServerLifecycle. */
public final class ServerServices implements AutoCloseable {
    private final MongoConnection connection;
    private final RuneCombatState combatState = new RuneCombatState();
    private final AdminApiServer adminApi;
    public final AdminAccessService adminAccess = new AdminAccessService(new FileAdminWhitelistRepository());
    public final ResourceService resources;
    public final RuneActionService runeActions;
    public final AdminOperationsService adminOperations;
    public final AdminQueryService adminQueries;
    public final ManagedMobService mobs;
    public final MobRewardService mobRewards;
    public final MobProgressionService mobProgression;
    public final MobZoneOverlayService mobZoneOverlay;
    public final StageTransitionService stageTransitions;
    public final StageTransitionTriggerService stageTransitionTriggers;
    public final PlayerSessionService sessions;
    public final PlayerStatsService playerStats;
    public final RuneCombatService combat;
    public final MobAdminService mobAdmin;
    public final RuneAdminService runeAdmin;
    public final PlayerCollectionService collections;
    public final RealmService realms = new RealmService();

    public ServerServices(Logger logger, boolean persistentGameplay) {
        connection = new MongoConnection(logger);
        PlayerRepository players = new MongoPlayerRepository(connection, persistentGameplay);
        MobRepository mobRepository = new MongoMobRepository(connection, persistentGameplay);
        LocationRepository locationRepository = new MongoLocationRepository(connection, persistentGameplay);
        MobProgressionRepository progressionRepository = new MongoMobProgressionRepository(connection, persistentGameplay);
        StageTransitionTriggerRepository triggerRepository = new MongoStageTransitionTriggerRepository(connection, persistentGameplay);
        StageTransitionPreferenceRepository transitionPreferences = new MongoStageTransitionPreferenceRepository(connection, persistentGameplay);
        RuneEnhancementRepository enhancements = new MongoRuneEnhancementRepository(connection);
        resources = new ResourceService(new MongoResourceRepository(connection));
        combat = new RuneCombatService(players, enhancements, combatState);
        RuneMobEffectService runeEffects = new RuneMobEffectService(combat, new RuneRewardBonusService(players, enhancements));
        playerStats = new PlayerStatsService(players, enhancements);
        PlayerStatsSyncService stats = new PlayerStatsSyncService(players, resources, playerStats, combat);
        sessions = new PlayerSessionService(players, resources, stats);
        RuneSyncService sync = new RuneSyncService(players, enhancements);
        runeActions = new RuneActionService(players, stats,
                new RunePurchaseService(players, resources, stats),
                new RuneEnhancementService(players, resources, stats, enhancements, sync),
                new RuneAwakeningService(players, resources, stats, new RuneAwakeningPaymentService(resources), sync), sync);
        mobProgression = new MobProgressionService(progressionRepository);
        stageTransitions = new StageTransitionService(locationRepository, mobProgression);
        stageTransitionTriggers = new StageTransitionTriggerService(
                triggerRepository, transitionPreferences, mobProgression, stageTransitions);
        mobRewards = new MobRewardService(resources, runeEffects, stats, players);
        mobs = new ManagedMobService(mobRepository, locationRepository, runeEffects, mobRewards, mobProgression);
        mobZoneOverlay = new MobZoneOverlayService(locationRepository);
        adminOperations = new AdminOperationsService(players, resources, stats, mobRepository, mobs, enhancements);
        adminQueries = new AdminQueryService(players, resources, mobRepository);
        adminApi = new AdminApiServer(logger, adminOperations, adminQueries);
        mobAdmin = new MobAdminService(mobRepository, locationRepository, mobs);
        runeAdmin = new RuneAdminService(players, resources, stats, enhancements);
        collections = new PlayerCollectionService(players, resources, stats);
    }

    public void connect() {
        connection.connect();
        stageTransitionTriggers.reload();
    }

    public void startAdminApi(MinecraftServer server, File configDirectory) throws IOException {
        adminApi.start(server, configDirectory);
    }

    @Override public void close() {
        adminApi.close();
        mobs.clear();
        mobProgression.clear();
        combatState.clear();
        connection.close();
    }
}
