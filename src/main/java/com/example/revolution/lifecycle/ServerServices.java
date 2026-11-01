package com.example.revolution.lifecycle;

import com.example.revolution.admin.api.AdminApiServer;
import com.example.revolution.admin.service.AdminAccessService;
import com.example.revolution.admin.service.AdminBalanceBatchService;
import com.example.revolution.admin.service.AdminOperationsService;
import com.example.revolution.admin.service.AdminQueryService;
import com.example.revolution.admin.service.AdminToolsService;
import com.example.revolution.admin.service.AdminStageBatchService;
import com.example.revolution.achievement.AchievementEconomyBonusService;
import com.example.revolution.achievement.AchievementService;
import com.example.revolution.achievement.BalanceAchievementCatalogFactory;
import com.example.revolution.achievement.BalanceAchievementPolicy;
import com.example.revolution.achievement.CanonicalAchievementRewardGateway;
import com.example.revolution.aura.AuraModifierService;
import com.example.revolution.aura.AuraService;
import com.example.revolution.aura.BalanceAuraPolicy;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.CompositeGameplayEventSink;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.god.BalanceGodWorshipPolicy;
import com.example.revolution.god.GodWorshipAutomationService;
import com.example.revolution.god.GodWorshipModel;
import com.example.revolution.god.GodWorshipModifierService;
import com.example.revolution.god.GodWorshipService;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.repository.FastAutoPreferenceRepository;
import com.example.revolution.location.repository.LocationGenerationRepository;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.example.revolution.location.repository.StageTransitionPreferenceRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.example.revolution.location.service.BootstrappingLocationGenerationRepository;
import com.example.revolution.location.service.FastAutoService;
import com.example.revolution.location.service.LocationProvisioningService;
import com.example.revolution.location.service.MobZoneOverlayService;
import com.example.revolution.location.service.StageMobBootstrapService;
import com.example.revolution.location.service.StageTransitionService;
import com.example.revolution.location.service.StageTransitionTriggerService;
import com.example.revolution.location.world.REBuilderAdapter;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.mob.repository.MobProgressionRepository;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.service.BalanceAwareMobRepository;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.mob.service.MobAdminService;
import com.example.revolution.mob.service.MobBalanceService;
import com.example.revolution.mob.service.MobProgressionService;
import com.example.revolution.mob.service.MobRewardService;
import com.example.revolution.persistence.FileAdminWhitelistRepository;
import com.example.revolution.persistence.MongoAchievementRepository;
import com.example.revolution.persistence.MongoAdminStageStore;
import com.example.revolution.persistence.MongoAuraRepository;
import com.example.revolution.persistence.MongoConnection;
import com.example.revolution.persistence.MongoFastAutoPreferenceRepository;
import com.example.revolution.persistence.MongoGodWorshipRepository;
import com.example.revolution.persistence.MongoLocationBuildTemplateRepository;
import com.example.revolution.persistence.MongoLocationGenerationRepository;
import com.example.revolution.persistence.MongoLocationRepository;
import com.example.revolution.persistence.MongoLocationStageTemplateRepository;
import com.example.revolution.persistence.MongoMechanicUnlockRepository;
import com.example.revolution.persistence.MongoMobProgressionRepository;
import com.example.revolution.persistence.MongoMobRepository;
import com.example.revolution.persistence.MongoPetRepository;
import com.example.revolution.persistence.MongoPlayerRepository;
import com.example.revolution.persistence.MongoPrestigeRepository;
import com.example.revolution.persistence.MongoQuestRepository;
import com.example.revolution.persistence.MongoResourceRepository;
import com.example.revolution.persistence.MongoRuneEnhancementRepository;
import com.example.revolution.persistence.MongoStageTransitionPreferenceRepository;
import com.example.revolution.persistence.MongoStageTransitionTriggerRepository;
import com.example.revolution.persistence.MongoUniqueRuneRepository;
import com.example.revolution.pet.definition.PetCatalog;
import com.example.revolution.pet.repository.PetRepository;
import com.example.revolution.pet.service.PetBalanceService;
import com.example.revolution.pet.service.PetEffectSource;
import com.example.revolution.pet.service.PetService;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerCollectionService;
import com.example.revolution.player.service.PlayerSessionService;
import com.example.revolution.player.service.PlayerStatsService;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.prestige.BalanceMasteryTierPolicy;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeProgressRouter;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.exchange.service.RunExchangeCatalogFactory;
import com.example.revolution.prestige.exchange.service.RunExchangeService;
import com.example.revolution.quest.service.QuestBalanceService;
import com.example.revolution.quest.service.QuestCatalog;
import com.example.revolution.quest.service.QuestGenerator;
import com.example.revolution.quest.service.QuestService;
import com.example.revolution.realm.service.RealmService;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.service.RuneActionService;
import com.example.revolution.rune.service.RuneAdminService;
import com.example.revolution.rune.service.RuneAwakeningPaymentService;
import com.example.revolution.rune.service.RuneAwakeningService;
import com.example.revolution.rune.service.RuneCombatService;
import com.example.revolution.rune.service.RuneEffectBalanceService;
import com.example.revolution.rune.service.RuneEnhancementBalanceService;
import com.example.revolution.rune.service.RuneEnhancementPlanningService;
import com.example.revolution.rune.service.RuneEnhancementService;
import com.example.revolution.rune.service.RuneMobEffectService;
import com.example.revolution.rune.service.RunePrestigeResetService;
import com.example.revolution.rune.service.RunePurchaseBalanceService;
import com.example.revolution.rune.service.RunePurchaseService;
import com.example.revolution.rune.service.RuneRankUpBalanceService;
import com.example.revolution.rune.service.RuneRewardBonusService;
import com.example.revolution.rune.service.RuneSyncService;
import com.example.revolution.rune.service.RuneTransientEffectRuntime;
import com.example.revolution.rune.state.RuneCombatState;
import com.example.revolution.unique.BalanceUniqueRunePolicy;
import com.example.revolution.unique.UniqueRuneModifierService;
import com.example.revolution.unique.UniqueRuneRepository;
import com.example.revolution.unique.UniqueRuneService;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** One object graph per server lifetime. Only adapters may retrieve it from ServerLifecycle. */
public final class ServerServices implements AutoCloseable {
    private final MongoConnection connection;
    private final Logger logger;
    private final RuneCombatState combatState = new RuneCombatState();
    private final AdminApiServer adminApi;
    private final StageMobBootstrapService stageMobs;
    private final MongoAdminStageStore adminStageStore;
    private final boolean persistentGameplay;
    public final AdminAccessService adminAccess = new AdminAccessService(new FileAdminWhitelistRepository());
    public final BalanceService balance;
    public final MechanicUnlockService mechanicUnlocks;
    public final ResourceService resources;
    public final RuneActionService runeActions;
    public final AdminOperationsService adminOperations;
    public final AdminQueryService adminQueries;
    public final ManagedMobService mobs;
    public final MobRewardService mobRewards;
    public final MobProgressionService mobProgression;
    public final MobZoneOverlayService mobZoneOverlay;
    public final LocationProvisioningService locationProvisioning;
    public final REBuilderAdapter physicalBuilds;
    public final StageTransitionService stageTransitions;
    public final StageTransitionTriggerService stageTransitionTriggers;
    public final FastAutoService fastAuto;
    public final PlayerSessionService sessions;
    public final PlayerStatsService playerStats;
    public final RuneCombatService combat;
    public final RuneTransientEffectRuntime runeTransientEffects;
    public final MobAdminService mobAdmin;
    public final RuneAdminService runeAdmin;
    public final PlayerCollectionService collections;
    public final RealmService realms = new RealmService();
    public final PrestigeService prestige;
    public final RunExchangeService runExchange;
    public final EffectResolver effects;
    public final GameplayEventSink gameplayEvents;
    public final QuestService quests;
    public final PetService pets;
    public final PetEffectSource petEffects;
    public final BalanceGodWorshipPolicy godPolicy;
    public final GodWorshipService gods;
    public final GodWorshipAutomationService godAutomation;
    public final GodWorshipModifierService godModifiers;
    public final BalanceAuraPolicy auraPolicy;
    public final AuraService auras;
    public final AuraModifierService auraModifiers;
    public final AchievementService achievements;
    public final BalanceAchievementPolicy achievementPolicy;
    public final AchievementEconomyBonusService achievementBonus;
    public final BalanceUniqueRunePolicy uniqueRunePolicy;
    public final UniqueRuneService uniqueRunes;
    public final UniqueRuneModifierService uniqueRuneModifiers;

    public ServerServices(Logger logger, boolean persistentGameplay, File configDirectory) {
        this.logger = logger;
        this.persistentGameplay = persistentGameplay;
        connection = new MongoConnection(logger);
        adminStageStore = new MongoAdminStageStore(connection, persistentGameplay);
        PlayerRepository players = new MongoPlayerRepository(connection, persistentGameplay);
        LocationRepository locationRepository = new MongoLocationRepository(connection, persistentGameplay);
        LocationStageTemplateRepository stageTemplateRepository = new MongoLocationStageTemplateRepository(connection, persistentGameplay);
        MobProgressionRepository progressionRepository = new MongoMobProgressionRepository(connection, persistentGameplay);
        StageTransitionTriggerRepository triggerRepository = new MongoStageTransitionTriggerRepository(connection, persistentGameplay);
        StageTransitionPreferenceRepository transitionPreferences = new MongoStageTransitionPreferenceRepository(connection, persistentGameplay);
        FastAutoPreferenceRepository fastAutoPreferences = new MongoFastAutoPreferenceRepository(connection, persistentGameplay);
        RuneEnhancementRepository enhancements = new MongoRuneEnhancementRepository(connection);
        PetRepository petRepository = new MongoPetRepository(connection, persistentGameplay);

        balance = BalanceBootstrap.activateBundledDefaults();
        mechanicUnlocks = new MechanicUnlockService(
                players,
                new MongoMechanicUnlockRepository(connection, persistentGameplay),
                new BalanceMechanicUnlockPolicy(balance));
        MongoGodWorshipRepository godRepository = new MongoGodWorshipRepository(connection, persistentGameplay);
        MongoAuraRepository auraRepository = new MongoAuraRepository(connection, persistentGameplay);
        UniqueRuneRepository uniqueRuneRepository = new MongoUniqueRuneRepository(connection, persistentGameplay);
        uniqueRunePolicy = new BalanceUniqueRunePolicy(balance);
        uniqueRuneModifiers = new UniqueRuneModifierService(uniqueRuneRepository, uniqueRunePolicy);
        godPolicy = new BalanceGodWorshipPolicy(balance);
        auraPolicy = new BalanceAuraPolicy(balance);
        PrestigeBranchRegistry prestigeBranches = new PrestigeBranchRegistry();
        RunePrestigeResetService runePrestigeReset = new RunePrestigeResetService(players, enhancements);
        prestige = new PrestigeService(
                new MongoPrestigeRepository(connection, persistentGameplay),
                prestigeBranches,
                (playerUuid, branch) -> prestigeBranches.get(branch).defaultSaveMultiplier,
                runePrestigeReset::reset,
                new BalanceMasteryTierPolicy(balance),
                (playerUuid, mode) -> {
                    if (!godPolicy.featureEnabled(GodWorshipModel.FeatureGate.GOD_RESET_PRESERVATION)
                            || !mechanicUnlocks.isUnlocked(playerUuid, BalanceMechanicUnlockPolicy.GOD_RESET_PRESERVATION)) {
                        godRepository.resetRunState(playerUuid);
                    }
                });
        prestige.setFragmentRewardProvider(branch -> new BigDecimal(balance.getActiveValue(
                "prestige.fragment_reward." + branch.name().toLowerCase(Locale.ROOT) + ".per_level")));
        effects = new EffectResolver();
        effects.register(prestige);
        resources = new ResourceService(new MongoResourceRepository(connection), effects);
        resources.setUniqueRuneRewardMultiplier(playerUuid ->
                mechanicUnlocks.isUnlocked(playerUuid, BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)
                        ? uniqueRuneModifiers.resourceMultiplier(playerUuid) : BigDecimal.ONE);

        achievementPolicy = new BalanceAchievementPolicy(balance);
        achievements = new AchievementService(
                new MongoAchievementRepository(connection, persistentGameplay),
                BalanceAchievementCatalogFactory.fromBalance(balance),
                new CanonicalAchievementRewardGateway(resources, godRepository));
        achievementBonus = new AchievementEconomyBonusService(achievements, achievementPolicy);
        resources.setAchievementRewardMultiplier(playerUuid ->
                mechanicUnlocks.isUnlocked(playerUuid, BalanceMechanicUnlockPolicy.ACHIEVEMENTS)
                        ? achievementBonus.multiplier(playerUuid) : BigDecimal.ONE);

        auras = new AuraService(auraRepository, resources, auraPolicy);
        auraModifiers = new AuraModifierService(auraRepository);
        resources.setLateRewardMultiplier(playerUuid ->
                mechanicUnlocks.isUnlocked(playerUuid, BalanceMechanicUnlockPolicy.AURA_SYSTEM)
                        ? auraModifiers.resourceMultiplier(playerUuid) : BigDecimal.ONE);
        gods = new GodWorshipService(godRepository, resources, godPolicy);
        godAutomation = new GodWorshipAutomationService(godRepository, godPolicy);
        godModifiers = new GodWorshipModifierService(godRepository, godPolicy);
        QuestBalanceService questBalance = new QuestBalanceService(balance);
        quests = new QuestService(
                new MongoQuestRepository(connection, persistentGameplay),
                resources,
                new QuestGenerator(new QuestCatalog(questBalance)),
                questBalance);
        gameplayEvents = new CompositeGameplayEventSink(
                new PrestigeProgressRouter(prestige, prestigeBranches),
                quests,
                achievements);
        prestige.setGameplayEventSink(gameplayEvents);
        resources.setGameplayEventSink(gameplayEvents);
        auras.setGameplayEventSink(gameplayEvents);
        gods.setGameplayEventSink(gameplayEvents);
        godAutomation.setGameplayEventSink(gameplayEvents);
        PetCatalog petCatalog = new PetCatalog();
        PetBalanceService petBalance = new PetBalanceService(balance);
        pets = new PetService(petRepository, petCatalog, petBalance, resources, prestige);
        pets.setGameplayEventSink(gameplayEvents);
        runExchange = new RunExchangeService(prestige, resources, pets, balance, RunExchangeCatalogFactory.fromBalance(balance));
        runExchange.setGameplayEventSink(gameplayEvents);
        petEffects = new PetEffectSource(petRepository, petCatalog, petBalance);
        effects.register(petEffects);

        MobRepository storedMobRepository = new MongoMobRepository(connection, persistentGameplay);
        MobRepository mobRepository = new BalanceAwareMobRepository(storedMobRepository, new MobBalanceService(balance));
        RuneEffectBalanceService effectBalance = new RuneEffectBalanceService(balance);
        combat = new RuneCombatService(players, enhancements, combatState, effectBalance);
        runeTransientEffects = new RuneTransientEffectRuntime(players, enhancements, effectBalance, combat);
        RuneMobEffectService runeEffects = new RuneMobEffectService(
                combat, new RuneRewardBonusService(players, enhancements, effectBalance));
        playerStats = new PlayerStatsService(players, enhancements);
        PlayerStatsSyncService stats = new PlayerStatsSyncService(players, resources, playerStats, combat);
        sessions = new PlayerSessionService(players, resources, stats);
        RuneEnhancementBalanceService enhancementBalance = new RuneEnhancementBalanceService(balance);
        RuneEnhancementPlanningService enhancementPlanning = new RuneEnhancementPlanningService(enhancementBalance);
        RuneRankUpBalanceService rankUpBalance = new RuneRankUpBalanceService(balance);
        RuneSyncService sync = new RuneSyncService(players, enhancements, enhancementBalance);
        RunePurchaseService runePurchase = new RunePurchaseService(players, resources, stats, new RunePurchaseBalanceService(balance));
        RuneEnhancementService runeEnhancement = new RuneEnhancementService(players, resources, stats, enhancements, sync, enhancementPlanning);
        RuneAwakeningService runeAwakening = new RuneAwakeningService(players, stats, rankUpBalance, new RuneAwakeningPaymentService(resources), sync);
        runePurchase.setGameplayEventSink(gameplayEvents);
        runeEnhancement.setGameplayEventSink(gameplayEvents);
        runeAwakening.setGameplayEventSink(gameplayEvents);
        runeActions = new RuneActionService(players, stats, runePurchase, runeEnhancement, runeAwakening, sync, mechanicUnlocks);
        mobProgression = new MobProgressionService(progressionRepository);

        uniqueRunes = new UniqueRuneService(
                uniqueRuneRepository,
                uniqueRunePolicy,
                resources,
                playerUuid -> {
                    long unlocked = 0L;
                    for (Integer worldLocationId : mobProgression.configuredWorldLocations()) {
                        for (Integer slot : mobProgression.configuredSlots(worldLocationId)) {
                            if (mobProgression.isUnlocked(playerUuid, MobZoneIds.encode(worldLocationId, slot))) unlocked++;
                        }
                    }
                    return unlocked;
                },
                playerUuid -> {
                    GodWorshipModel.RunState run = godRepository.loadRunState(playerUuid);
                    for (GodWorshipModel.State state : run.gods.values()) if (state.unlocked) return true;
                    return false;
                },
                new UniqueRuneService.OfferingsWallet() {
                    @Override public BigDecimal balance(java.util.UUID playerUuid) {
                        GodWorshipModel.PersistentState state = godRepository.loadPersistentState(playerUuid);
                        return state.offerings == null ? BigDecimal.ZERO : state.offerings;
                    }
                    @Override public synchronized boolean spend(java.util.UUID playerUuid, BigDecimal amount) {
                        if (amount == null || amount.signum() < 0) return false;
                        GodWorshipModel.PersistentState state = godRepository.loadPersistentState(playerUuid);
                        BigDecimal current = state.offerings == null ? BigDecimal.ZERO : state.offerings;
                        if (current.compareTo(amount) < 0) return false;
                        state.offerings = current.subtract(amount);
                        godRepository.savePersistentState(playerUuid, state);
                        return true;
                    }
                });
        uniqueRunes.setGameplayEventSink(gameplayEvents);

        physicalBuilds = new REBuilderAdapter(configDirectory.toPath());
        LocationGenerationRepository storedGenerations = new MongoLocationGenerationRepository(connection, persistentGameplay);
        stageMobs = new StageMobBootstrapService(mobRepository, locationRepository, storedGenerations);
        LocationGenerationRepository bootstrappingGenerations =
                new BootstrappingLocationGenerationRepository(storedGenerations, stageMobs);
        locationProvisioning = new LocationProvisioningService(
                locationRepository,
                new LocationGenerationPlanner(),
                LocationTemplate.defaultStage(),
                stageTemplateRepository,
                triggerRepository,
                new MongoLocationBuildTemplateRepository(connection, persistentGameplay),
                physicalBuilds,
                bootstrappingGenerations);
        stageTransitions = new StageTransitionService(locationRepository, mobProgression);
        stageTransitions.setGameplayEventSink(gameplayEvents);
        stageTransitionTriggers = new StageTransitionTriggerService(
                triggerRepository, transitionPreferences, locationRepository, mobProgression, stageTransitions);
        fastAuto = new FastAutoService(fastAutoPreferences, mobProgression);
        mobProgression.setUnlockListener((playerUuid, result) -> {
            fastAuto.onUnlocked(playerUuid, result);
            gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.STAGES_COMPLETED));
        });
        mobRewards = new MobRewardService(resources, runeEffects, stats, players);
        mobRewards.setGameplayEventSink(gameplayEvents);
        mobs = new ManagedMobService(mobRepository, locationRepository, runeEffects, mobRewards, mobProgression);
        if (persistentGameplay) mobs.setConfigurationGuard(adminStageStore::recover);
        mobZoneOverlay = new MobZoneOverlayService(locationRepository);
        adminOperations = new AdminOperationsService(players, resources, stats, mobRepository, mobs, enhancements, sync);
        adminQueries = new AdminQueryService(players, resources, mobRepository);
        if (persistentGameplay) adminQueries.setConfigurationGuard(adminStageStore::recover);
        adminApi = new AdminApiServer(logger, adminOperations, adminQueries);
        AdminToolsService adminTools = new AdminToolsService(locationRepository, mobRepository, mobProgression);
        if (persistentGameplay) adminTools.setConfigurationGuard(adminStageStore::recover);
        adminApi.setTools(adminTools, new AdminStageBatchService(adminStageStore, adminTools::stages, mobs::respawnAll));
        adminApi.setBalance(balance, new AdminBalanceBatchService(balance));
        mobAdmin = new MobAdminService(mobRepository, locationRepository, mobs);
        runeAdmin = new RuneAdminService(players, resources, stats, enhancements, sync);
        collections = new PlayerCollectionService(players, resources, stats);
    }

    public void connect() {
        connection.connect();
        if (persistentGameplay) adminStageStore.recover();
        int reconciled = stageMobs.reconcileConfiguredRange(1, 1, 41);
        if (reconciled > 0) logger.info("Canonical Location 1 mob bootstrap reconciled {} stage(s)", reconciled);
        stageTransitionTriggers.reload();
    }

    public void startAdminApi(MinecraftServer server, File configDirectory) throws IOException {
        adminApi.start(server, configDirectory);
    }

    @Override public void close() {
        adminApi.close();
        achievements.flushAll();
        runeTransientEffects.clear();
        mobs.clear();
        fastAuto.clear();
        mobProgression.clear();
        combatState.clear();
        connection.close();
    }
}
