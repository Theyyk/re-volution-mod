package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
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

    @Test void ordinaryLastNormalStageDoesNotCompleteLocation() {
        List<LocationDefinition> zones = Arrays.asList(zone(1, 2), zone(1, 5), zone(1, 9), zone(2, 2), bossZone(2, 9));
        List<StoredMobSpawn> spawns = Arrays.asList(spawn(1, 2, true), spawn(1, 5, true), spawn(1, 9, true), spawn(2, 2, true), spawn(2, 9, true));
        service.reloadConfiguration(zones, spawns);
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertNotNull(service.unlockNext(player, id(1, 5)));
        assertFalse(service.isFinalStage(id(1, 9)));
        assertNull(service.unlockNext(player, id(1, 9)));
        assertFalse(service.isUnlocked(player, id(2, 2)));
        assertNull(service.nextConfiguredInternalLocationId(id(1, 9)));
    }

    @Test void bossMustBeFinalConfiguredStageToCompleteLocation() {
        List<LocationDefinition> zones = Arrays.asList(zone(1, 2), bossZone(1, 5), zone(1, 9), zone(2, 2));
        List<StoredMobSpawn> spawns = Arrays.asList(spawn(1, 2, true), spawn(1, 5, true), spawn(1, 9, true), spawn(2, 2, true));
        service.reloadConfiguration(zones, spawns);
        assertNotNull(service.unlockNext(player, id(1, 2)));
        assertTrue(service.isBossStage(id(1, 5)));
        assertFalse(service.isFinalStage(id(1, 5)));
        assertNotNull(service.unlockNext(player, id(1, 5))); // ordinary in-location progression still reaches slot 9
        assertNull(service.unlockNext(player, id(1, 9)));
        assertFalse(service.isUnlocked(player, id(2, 2)));
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

    @Test void configurationIgnoresDisabledMissingAndDuplicateSpawnStages() {
        service.reloadConfiguration(Arrays.asList(zone(4, 2), bossZone(4, 5)), Arrays.asList(
                spawn(4, 2, true), spawn(4, 2, true), spawn(4, 5, false), spawn(4, 9, true)));
        assertEquals(Collections.singletonList(2), service.configuredSlots(4));
        assertFalse(service.isFinalStage(id(4, 2)));
        assertNull(service.unlockNext(player, id(4, 2)));
        assertThrows(UnsupportedOperationException.class, () -> service.configuredSlots(4).add(5));
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
