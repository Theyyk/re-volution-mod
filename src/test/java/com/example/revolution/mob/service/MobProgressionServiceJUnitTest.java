package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.service.LocationResetReturnService;
import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobProgressionServiceJUnitTest {
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-000000001310");
    private InMemoryMobProgressionRepository repository;
    private MobProgressionService service;

    @BeforeEach void setUp() {
        repository = new InMemoryMobProgressionRepository();
        service = new MobProgressionService(repository);
        loadThreeLocations();
    }

    @Test void onlyFirstConfiguredLocationStartsOpen() {
        assertTrue(service.isUnlocked(player, id(1, 2)));
        assertFalse(service.isUnlocked(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(3, 2)));
    }

    @Test void adminGrantAfterLegacyMaskDoesNotReopenUnrelatedRunEarnedStages() {
        repository.seed(player, 1, 2, 5, 9);
        assertTrue(service.classifyLegacyRunProgress(player));
        assertFalse(service.isUnlocked(player, id(1, 9)));
        assertTrue(service.grantStage(player, 1, 5));
        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertFalse(service.isUnlocked(player, id(1, 9)));
        // Existing historical grants must remain masked even after an admin writes.
        assertTrue(repository.rawLegacyContains(player, 1, 9));
        assertTrue(service.classifyLegacyRunProgress(player));
        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertFalse(service.isUnlocked(player, id(1, 9)));
        assertEquals(0, repository.replaceWrites);
    }

    @Test void adminGrantInvalidatesCacheWithoutAutomaticTravel() {
        assertFalse(service.isUnlocked(player, id(2, 9)));
        int[] events = {0}; service.setUnlockListener((uuid, result) -> events[0]++);
        assertTrue(service.grantStage(player, 2, 9));
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertTrue(service.isUnlocked(player, id(2, 9)));
        assertTrue(service.isUnlocked(player, id(2, 5)));
        assertEquals(0, events[0]);
        assertFalse(service.grantStage(player, 2, 99));
    }

    @Test void adminGrantStageFortyUnlocksEveryConfiguredStageThroughForty() {
        List<LocationDefinition> zones = new ArrayList<>();
        List<StoredMobSpawn> spawns = new ArrayList<>();
        for (int slot = 1; slot <= 41; slot++) {
            zones.add(slot == 41 ? bossZone(1, slot) : zone(1, slot));
            spawns.add(spawn(1, slot, true));
        }
        service.reloadConfiguration(zones, spawns);
        assertTrue(service.grantStage(player, 1, 40));
        for (int slot = 1; slot <= 40; slot++) assertTrue(service.isUnlocked(player, id(1, slot)), "stage " + slot);
        assertFalse(service.isUnlocked(player, id(1, 41)));
    }

    @Test void lowerAdminGrantDoesNotRemoveAlreadyUnlockedLaterStage() {
        assertTrue(service.grantStage(player, 2, 9));
        assertTrue(service.grantStage(player, 2, 5));
        assertTrue(service.isUnlocked(player, id(2, 9)));
    }

    @Test void adminResetExactlyOneLocationRetainsOtherLocationRecords() {
        assertTrue(service.grantStage(player, 2, 9));
        assertTrue(service.grantStage(player, 3, 9));
        assertTrue(service.resetLocation(player, 2));
        assertFalse(service.isUnlocked(player, id(2, 9)));
        assertTrue(service.isUnlocked(player, id(3, 9)));
        assertTrue(service.resetFromLocation(player, 0));
        assertFalse(service.isUnlocked(player, id(3, 9)));
        assertTrue(service.isUnlocked(player, id(1, 2)));
    }

    @Test void explicitBossUnlocksFirstStageOfNextLocation() {
        unlockLocationOneThroughBoss();
        assertTrue(service.isBossStage(id(1, 9)));
        assertTrue(service.isFinalStage(id(1, 9)));
        assertFalse(service.isUnlocked(player, id(2, 2)));
        MobProgressionService.UnlockResult result = service.unlockNext(player, id(1, 9));
        assertNotNull(result);
        assertEquals(2, result.worldLocationId);
        assertEquals(2, result.mobSlotId);
        assertEquals("stage-2-2", result.mobName);
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertEquals(Integer.valueOf(id(2, 2)), service.nextUnlockedInternalLocationId(player, id(1, 9)));
        assertEquals(Integer.valueOf(id(1, 9)), service.previousUnlockedInternalLocationId(player, id(2, 2)));
    }

    @Test void bossUnlocksEmptyFirstStageOfNextLocation() {
        List<LocationDefinition> zones = Arrays.asList(
                zone(1, 1), zone(1, 2), bossZone(1, 3), zone(2, 1));
        List<StoredMobSpawn> spawns = Arrays.asList(
                spawn(1, 1, true), spawn(1, 2, true), spawn(1, 3, true));
        service.reloadConfiguration(zones, spawns);

        assertEquals(Arrays.asList(1, 2, 3), service.configuredSlots(1));
        assertEquals(Collections.singletonList(1), service.configuredSlots(2));
        assertNotNull(service.unlockNext(player, id(1, 1)));
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertFalse(service.isUnlocked(player, id(2, 1)));

        MobProgressionService.UnlockResult result = service.unlockNext(player, id(1, 3));
        assertNotNull(result);
        assertEquals(2, result.worldLocationId);
        assertEquals(1, result.mobSlotId);
        assertEquals("", result.mobName);
        assertTrue(service.isUnlocked(player, id(2, 1)));
    }

    @Test void ordinaryLastStageCompletesLocationAutomatically() {
        List<LocationDefinition> zones = Arrays.asList(zone(1, 2), zone(1, 5), zone(1, 9), zone(2, 2), bossZone(2, 9));
        List<StoredMobSpawn> spawns = Arrays.asList(spawn(1, 2, true), spawn(1, 5, true), spawn(1, 9, true), spawn(2, 2, true), spawn(2, 9, true));
        service.reloadConfiguration(zones, spawns);
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        assertTrue(service.isBossStage(id(1, 9)));
        assertTrue(service.isFinalStage(id(1, 9)));
        assertNotNull(service.unlockNext(player, id(1, 9)));
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertEquals(Integer.valueOf(id(2, 2)), service.nextConfiguredInternalLocationId(id(1, 9)));
    }

    @Test void persistedEarlierBossDoesNotOverrideAutomaticFinalBoss() {
        List<LocationDefinition> zones = Arrays.asList(zone(1, 2), bossZone(1, 5), zone(1, 9), zone(2, 2));
        List<StoredMobSpawn> spawns = Arrays.asList(spawn(1, 2, true), spawn(1, 5, true), spawn(1, 9, true), spawn(2, 2, true));
        service.reloadConfiguration(zones, spawns);
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertFalse(service.isBossStage(id(1, 5)));
        assertFalse(service.isFinalStage(id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        assertTrue(service.isBossStage(id(1, 9)));
        assertTrue(service.isFinalStage(id(1, 9)));
        assertNotNull(service.unlockNext(player, id(1, 9)));
        assertTrue(service.isUnlocked(player, id(2, 2)));
    }

    @Test void lastConfiguredBossStopsCleanlyWhenFutureLocationDoesNotExist() {
        unlockAll();
        assertTrue(service.isFinalStage(id(3, 9)));
        int writes = repository.unlockWrites;
        assertNull(service.unlockNext(player, id(3, 9)));
        assertNull(service.nextConfiguredInternalLocationId(id(3, 9)));
        assertEquals(writes, repository.unlockWrites);
    }

    @Test void failedUnlockDoesNotOpenNavigationOrPoisonRetry() {
        openLocation(2);
        repository.failUnlock = true;
        assertNull(service.unlockNext(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(2, 5)));
        assertNull(service.nextUnlockedInternalLocationId(player, id(2, 2)));
        repository.failUnlock = false;
        assertNotNull(service.unlockNext(player, id(2, 2)));
        assertEquals(Integer.valueOf(id(2, 5)), service.nextUnlockedInternalLocationId(player, id(2, 2)));
    }

    @Test void repeatedUnlockDoesNotWriteOrNotifyTwice() {
        openLocation(2);
        final int[] notifications = {0};
        service.setUnlockListener((uuid, result) -> notifications[0]++);
        assertNotNull(service.unlockNext(player, id(2, 2)));
        assertNull(service.unlockNext(player, id(2, 2)));
        assertEquals(2, repository.unlockWrites);
        assertEquals(1, notifications[0]);
    }

    @Test void lockedFutureLocationCannotUnlockItsOwnStages() {
        assertNull(service.unlockNext(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(2, 5)));
    }

    @Test void orphanLaterStageRecordCannotBypassLocationOpener() {
        repository.seed(player, 2, 5);
        service.reloadConfiguration(allZones(), allSpawns());
        assertFalse(service.isUnlocked(player, id(2, 5)));
    }

    @Test void revokeClosesRequestedStageAndEveryLaterLocation() {
        unlockAll();
        assertTrue(service.revokeFrom(player, 2, 5));
        assertTrue(service.isUnlocked(player, id(1, 9)));
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(2, 5)));
        assertFalse(service.isUnlocked(player, id(2, 9)));
        assertFalse(service.isUnlocked(player, id(3, 2)));
        assertFalse(service.isUnlocked(player, id(3, 9)));
    }

    @Test void resetFromLaterLocationDoesNotReopenItsFirstStage() {
        unlockAll();
        assertTrue(service.resetFromLocation(player, 2));
        assertTrue(service.isUnlocked(player, id(1, 9)));
        assertFalse(service.isUnlocked(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(3, 2)));
    }

    @Test void failedRevokeReplacementDoesNotClearLaterLocations() {
        unlockAll();
        repository.failReplace = true;
        assertFalse(service.revokeFrom(player, 2, 5));
        assertEquals(0, repository.clearWrites);
        assertTrue(service.isUnlocked(player, id(2, 9)));
        assertTrue(service.isUnlocked(player, id(3, 9)));
    }

    @Test void failedResetRetainsCachedAndPersistedUnlocksAndCanBeRetried() {
        unlockAll();
        repository.failClear = true;
        assertFalse(service.resetFromLocation(player, 2));
        assertTrue(service.isUnlocked(player, id(2, 9)));
        repository.failClear = false;
        assertTrue(service.resetFromLocation(player, 2));
        assertFalse(service.isUnlocked(player, id(2, 2)));
    }

    @Test void configuredResolverCrossesLocationBoundaryBothWaysThroughBoss() {
        assertEquals(Integer.valueOf(id(2, 2)), service.nextConfiguredInternalLocationId(id(1, 9)));
        assertEquals(Integer.valueOf(id(1, 9)), service.previousConfiguredInternalLocationId(id(2, 2)));
        assertEquals(Integer.valueOf(id(1, 5)), service.previousConfiguredInternalLocationId(id(1, 9)));
        assertEquals(Integer.valueOf(id(2, 5)), service.nextConfiguredInternalLocationId(id(2, 2)));
    }

    @Test void invalidMutationRequestsNeverWrite() {
        assertNull(service.unlockNext(null, id(1, 2)));
        assertNull(service.unlockNext(player, 1));
        assertFalse(service.revokeFrom(player, 1, 3));
        assertFalse(service.revokeFrom(null, 1, 2));
        assertFalse(service.resetFromLocation(player, -1));
        assertFalse(service.resetFromLocation(null, 1));
        assertEquals(0, repository.unlockWrites + repository.replaceWrites + repository.clearWrites);
    }

    @Test void zoneDefinitionsRemainConfiguredWithoutEnabledPopulation() {
        service.reloadConfiguration(Arrays.asList(zone(4, 2), bossZone(4, 5)), Arrays.asList(
                spawn(4, 2, true), spawn(4, 2, true), spawn(4, 5, false), spawn(4, 9, true)));
        assertEquals(Arrays.asList(2, 5), service.configuredSlots(4));
        assertTrue(service.isFinalStage(id(4, 5)));
        MobProgressionService.UnlockResult result = service.unlockNext(player, id(4, 2));
        assertNotNull(result);
        assertEquals(5, result.mobSlotId);
        assertEquals("", result.mobName);
        assertNull(service.unlockNext(player, id(4, 5)));
        assertThrows(UnsupportedOperationException.class, () -> service.configuredSlots(4).add(9));
    }

    @Test void clearEvictsCachedUnlocksBeforeFreshConfiguration() {
        repository.seed(player, 2, 5);
        assertFalse(service.isUnlocked(player, id(2, 5)));
        service.clear();
        assertTrue(service.configuredSlots(2).isEmpty());
        repository.seed(player, 2);
        loadThreeLocations();
        assertFalse(service.isUnlocked(player, id(2, 5)));
    }

    @Test void startingZoneIsFirstConfiguredStageAndCanLaterBeReplaced() {
        LocationResetReturnService returns = new LocationResetReturnService(null, service, null, null, null);
        assertEquals(id(1, 2), returns.startingStageId());
        service.reloadConfiguration(Arrays.asList(zone(4, 3), zone(4, 8)), Collections.emptyList());
        assertEquals(id(4, 3), returns.startingStageId());
        service.clear();
        assertThrows(IllegalStateException.class, returns::startingStageId);
    }

    @Test void repeatClassificationReconcilesNewlyDiscoveredUnmaskedOldStages() {
        UUID unaffected = UUID.fromString("00000000-0000-0000-0000-000000009033");
        repository.seed(player, 1, 2);
        repository.seed(unaffected, 1, 5, 9);
        assertTrue(service.classifyLegacyRunProgress(player));
        assertFalse(service.isUnlocked(player, id(1, 5)));
        // Simulate a partially migrated historical row that later exposes more old slots.
        repository.seed(player, 1, 2, 5, 9);
        // A direct storage mutation does not invalidate the service cache; the admin
        // reconciliation itself must refresh it and mask the newly discovered slots.
        assertTrue(service.classifyLegacyRunProgress(player));
        assertEquals("LOCKED", service.stageAccessSource(player, id(1, 5)));
        assertEquals("LOCKED", service.stageAccessSource(player, id(1, 9)));
        assertTrue(repository.rawLegacyContains(player, 1, 9));
        assertTrue(service.isUnlocked(unaffected, id(1, 5)));
        // A later confirmed admin grant remains permanent despite reclassification.
        assertTrue(service.grantStage(player, 1, 5));
        assertTrue(service.classifyLegacyRunProgress(player));
        assertEquals("PERMANENT_OR_LEGACY", service.stageAccessSource(player, id(1, 5)));
        assertEquals("LOCKED", service.stageAccessSource(player, id(1, 9)));
    }

    @Test void confirmedHistoricalOrdinaryStagesAreMaskedWithoutDeletingOtherPlayersGrants() {
        UUID otherPlayer = UUID.fromString("00000000-0000-0000-0000-000000001311");
        repository.seed(player, 1, 5, 9);
        repository.seed(otherPlayer, 1, 5, 9);
        service.reloadConfiguration(allZones(), allSpawns());

        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertTrue(service.isUnlocked(otherPlayer, id(1, 5)));

        assertTrue(service.classifyLegacyRunProgress(player));
        assertTrue(service.isUnlocked(player, id(1, 2)));
        assertFalse(service.isUnlocked(player, id(1, 5)));
        assertFalse(service.isUnlocked(player, id(1, 9)));
        assertTrue(service.isUnlocked(otherPlayer, id(1, 5)));
        assertTrue(service.isUnlocked(otherPlayer, id(1, 9)));
        assertTrue(repository.rawLegacyContains(player, 1, 5));

        assertTrue(service.classifyLegacyRunProgress(player));
        assertFalse(service.isUnlocked(player, id(1, 5)));
        assertTrue(service.grantStage(player, 1, 5));
        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertFalse(service.isUnlocked(player, id(1, 9)));
    }

    @Test void failedHistoricalClassificationCanBeSafelyRetried() {
        repository.seed(player, 1, 5);
        service.reloadConfiguration(allZones(), allSpawns());
        repository.failClassification = true;
        assertFalse(service.classifyLegacyRunProgress(player));
        assertTrue(service.isUnlocked(player, id(1, 5)));
        repository.failClassification = false;
        assertTrue(service.classifyLegacyRunProgress(player));
        assertFalse(service.isUnlocked(player, id(1, 5)));
    }

        @Test void configuredStagesUnlockOutsideMasteryRunWithoutPermanentWrite() {
        final java.util.Set<Integer> cycleStages = new java.util.HashSet<>();
        final boolean[] active = {false};
        service.setRunStageAccess(new MobProgressionService.RunStageAccess() {
            @Override public boolean isRunActive(UUID uuid) { return active[0]; }
            @Override public boolean isRunStageUnlocked(UUID uuid, int id) { return cycleStages.contains(id); }
            @Override public boolean unlockRunStage(UUID uuid, int id) { return cycleStages.add(id); }
        });
        assertEquals("START", service.stageAccessSource(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertEquals("CYCLE", service.stageAccessSource(player, id(1, 5)));
        assertEquals(0, repository.unlockWrites);
        // Choosing a Mastery Run later does not remove previously opened stages.
        active[0] = true;
        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        active[0] = false;
        assertTrue(service.isUnlocked(player, id(1, 9)));
        // Reset clears cycle state, not the permanent mob progression repository.
        cycleStages.clear();
        assertEquals("LOCKED", service.stageAccessSource(player, id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertEquals("CYCLE", service.stageAccessSource(player, id(1, 5)));
        assertEquals(0, repository.unlockWrites);
    }

    @Test void stageAccessAuditReflectsLegacyClassificationAndNewAdminGrant() {
        repository.seed(player, 1, 5);
        service.reloadConfiguration(allZones(), allSpawns());
        assertEquals("PERMANENT_OR_LEGACY", service.stageAccessSource(player, id(1, 5)));
        assertTrue(service.classifyLegacyRunProgress(player));
        assertEquals("LOCKED", service.stageAccessSource(player, id(1, 5)));
        assertTrue(service.grantStage(player, 1, 5));
        assertEquals("PERMANENT_OR_LEGACY", service.stageAccessSource(player, id(1, 5)));
    }

    @Test void runOnlyStageUnlockDoesNotOverwritePermanentGrants() {
        final java.util.Set<Integer> runStages = new java.util.HashSet<>();
        final boolean[] active = {true};
        service.setRunStageAccess(new MobProgressionService.RunStageAccess() {
            @Override public boolean isRunActive(UUID uuid) { return active[0]; }
            @Override public boolean isRunStageUnlocked(UUID uuid, int stage) { return runStages.contains(stage); }
            @Override public boolean unlockRunStage(UUID uuid, int stage) { return runStages.add(stage); }
        });
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertTrue(service.isUnlocked(player, id(1, 5)));
        assertEquals(0, repository.unlockWrites);
        assertTrue(service.grantStage(player, 2, 5));
        assertTrue(service.isUnlocked(player, id(2, 5)));
        runStages.clear();
        active[0] = false;
        assertFalse(service.isUnlocked(player, id(1, 5)));
        assertTrue(service.isUnlocked(player, id(2, 5)));
        assertTrue(service.isUnlocked(player, id(1, 2)));
    }

    @Test void runUnlockOfNextLocationFirstStageIsVisibleAndReversible() {
        final java.util.Set<Integer> runStages = new java.util.HashSet<>();
        service.setRunStageAccess(new MobProgressionService.RunStageAccess() {
            @Override public boolean isRunActive(UUID uuid) { return true; }
            @Override public boolean isRunStageUnlocked(UUID uuid, int stage) { return runStages.contains(stage); }
            @Override public boolean unlockRunStage(UUID uuid, int stage) { return runStages.add(stage); }
        });
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 9)));
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertEquals(0, repository.unlockWrites);
        runStages.clear();
        assertFalse(service.isUnlocked(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(1, 5)));
    }

    private void unlockLocationOneThroughBoss() {
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
    }

    private void openLocation(int location) {
        assertTrue(repository.unlockSlot(player, location, 2));
        service.reloadConfiguration(allZones(), allSpawns());
    }

    private void unlockAll() {
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 9)));
        assertNotNull(service.unlockNext(player, id(2, 2)));
        assertNotNull(service.unlockNext(player, id(2, 5)));
        assertNotNull(service.unlockNext(player, id(2, 9)));
        assertNotNull(service.unlockNext(player, id(3, 2)));
        assertNotNull(service.unlockNext(player, id(3, 5)));
    }

    private List<LocationDefinition> allZones() {
        List<LocationDefinition> zones = new ArrayList<>();
        for (int location : Arrays.asList(1, 2, 3)) {
            zones.add(zone(location, 2));
            zones.add(zone(location, 5));
            zones.add(bossZone(location, 9));
        }
        return zones;
    }

    private List<StoredMobSpawn> allSpawns() {
        List<StoredMobSpawn> spawns = new ArrayList<>();
        for (int location : Arrays.asList(1, 2, 3)) for (int slot : Arrays.asList(9, 2, 5)) spawns.add(spawn(location, slot, true));
        return spawns;
    }

    private void loadThreeLocations() { service.reloadConfiguration(allZones(), allSpawns()); }
    private static int id(int location, int slot) { return MobZoneIds.encode(location, slot); }
    private static LocationDefinition zone(int location, int slot) {
        return new LocationDefinition(id(location, slot), "stage", slot * 20.0D, location * 100.0D, 4.0D, LocationKind.MOB_ZONE);
    }
    private static LocationDefinition bossZone(int location, int slot) {
        return new LocationDefinition(id(location, slot), "boss", slot * 20.0D, location * 100.0D, 4.0D, LocationKind.BOSS_ZONE);
    }
    private static StoredMobSpawn spawn(int location, int slot, boolean enabled) {
        return new StoredMobSpawn(id(location, slot), slot, "stage-" + location + "-" + slot,
                slot * 20.0D, 64.0D, location * 100.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, enabled);
    }
}
