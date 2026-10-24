package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
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
    private final UUID other = UUID.fromString("00000000-0000-0000-0000-000000001311");
    private InMemoryMobProgressionRepository repository;
    private MobProgressionService service;

    @BeforeEach void setUp() {
        repository = new InMemoryMobProgressionRepository();
        service = new MobProgressionService(repository);
        loadThreeLocations();
    }

    @Test void failedUnlockDoesNotOpenNavigationOrPoisonRetry() {
        repository.failUnlock = true;
        assertNull(service.unlockNext(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(2, 5)));
        assertNull(service.nextUnlockedInternalLocationId(player, id(2, 2)));
        repository.failUnlock = false;
        assertNotNull(service.unlockNext(player, id(2, 2)));
        assertEquals(Integer.valueOf(id(2, 5)), service.nextUnlockedInternalLocationId(player, id(2, 2)));
        assertEquals(Collections.singleton(5), repository.getUnlockedSlots(player, 2));
    }

    @Test void repeatedUnlockDoesNotWriteOrNotifyTwice() {
        MobProgressionService.UnlockResult result = service.unlockNext(player, id(2, 2));
        assertNotNull(result);
        assertEquals(2, result.worldLocationId);
        assertEquals(5, result.mobSlotId);
        assertEquals("stage-2-5", result.mobName);
        assertNull(service.unlockNext(player, id(2, 2)));
        assertEquals(1, repository.unlockWrites);
    }

    @Test void revokeInvalidatesWarmLaterCachesAndPreservesEarlierLocationsAndOtherPlayer() {
        seedAndWarmBothPlayers();
        assertTrue(service.revokeFrom(player, 2, 5));
        assertTrue(service.isUnlocked(player, id(1, 9)));
        assertTrue(service.isUnlocked(player, id(2, 2)));
        assertFalse(service.isUnlocked(player, id(2, 5)));
        assertFalse(service.isUnlocked(player, id(2, 9)));
        assertFalse(service.isUnlocked(player, id(3, 5)));
        assertFalse(service.isUnlocked(player, id(3, 9)));
        assertNull(service.nextUnlockedInternalLocationId(player, id(2, 2)));
        assertTrue(service.isUnlocked(other, id(2, 9)));
        assertTrue(service.isUnlocked(other, id(3, 9)));
    }

    @Test void resetInvalidatesWarmCachesFromInclusiveLocationOnly() {
        seedAndWarmBothPlayers();
        assertTrue(service.resetFromLocation(player, 2));
        assertTrue(service.isUnlocked(player, id(1, 9)));
        for (int location : Arrays.asList(2, 3)) {
            assertTrue(service.isUnlocked(player, id(location, 2)));
            assertFalse(service.isUnlocked(player, id(location, 5)));
            assertFalse(service.isUnlocked(player, id(location, 9)));
            assertTrue(repository.getUnlockedSlots(player, location).isEmpty());
            assertTrue(service.isUnlocked(other, id(location, 9)));
        }
    }

    @Test void failedRevokeReplacementDoesNotClearLaterLocations() {
        seedAndWarmBothPlayers();
        repository.failReplace = true;
        assertFalse(service.revokeFrom(player, 2, 5));
        assertEquals(0, repository.clearWrites);
        assertTrue(service.isUnlocked(player, id(2, 9)));
        assertTrue(service.isUnlocked(player, id(3, 9)));
    }

    @Test void failedResetRetainsCachedAndPersistedUnlocksAndCanBeRetried() {
        seedAndWarmBothPlayers();
        repository.failClear = true;
        assertFalse(service.resetFromLocation(player, 2));
        assertTrue(service.isUnlocked(player, id(2, 9)));
        assertEquals(new java.util.HashSet<>(Arrays.asList(5, 9)), repository.getUnlockedSlots(player, 2));
        repository.failClear = false;
        assertTrue(service.resetFromLocation(player, 2));
        assertFalse(service.isUnlocked(player, id(2, 9)));
    }

    @Test void invalidMutationRequestsNeverWrite() {
        assertNull(service.unlockNext(null, id(1, 2)));
        assertNull(service.unlockNext(player, 1));
        assertNull(service.unlockNext(player, id(1, 9)));
        assertFalse(service.revokeFrom(player, 1, 3));
        assertFalse(service.revokeFrom(null, 1, 2));
        assertFalse(service.resetFromLocation(player, -1));
        assertFalse(service.resetFromLocation(null, 1));
        assertEquals(0, repository.unlockWrites + repository.replaceWrites + repository.clearWrites);
    }

    @Test void configurationIgnoresDisabledMissingAndDuplicateSpawnStages() {
        service.reloadConfiguration(Arrays.asList(zone(4, 2), zone(4, 5)), Arrays.asList(
                spawn(4, 2, true), spawn(4, 2, true), spawn(4, 5, false), spawn(4, 9, true)));
        assertEquals(Collections.singletonList(2), service.configuredSlots(4));
        assertNull(service.unlockNext(player, id(4, 2)));
        assertThrows(UnsupportedOperationException.class, () -> service.configuredSlots(4).add(5));
    }

    @Test void clearEvictsCachedUnlocksBeforeFreshConfiguration() {
        repository.seed(player, 2, 5);
        assertTrue(service.isUnlocked(player, id(2, 5)));
        service.clear();
        assertTrue(service.configuredSlots(2).isEmpty());
        repository.seed(player, 2);
        loadThreeLocations();
        assertFalse(service.isUnlocked(player, id(2, 5)));
    }

    private void seedAndWarmBothPlayers() {
        for (UUID user : Arrays.asList(player, other)) {
            for (int location : Arrays.asList(1, 2, 3)) {
                repository.seed(user, location, 5, 9);
                assertTrue(service.isUnlocked(user, id(location, 9)));
            }
        }
    }

    private void loadThreeLocations() {
        List<LocationDefinition> zones = new ArrayList<>();
        List<StoredMobSpawn> spawns = new ArrayList<>();
        for (int location : Arrays.asList(1, 2, 3)) {
            for (int slot : Arrays.asList(9, 2, 5)) {
                zones.add(zone(location, slot));
                spawns.add(spawn(location, slot, true));
            }
        }
        service.reloadConfiguration(zones, spawns);
    }

    private static int id(int location, int slot) { return MobZoneIds.encode(location, slot); }
    private static LocationDefinition zone(int location, int slot) {
        return new LocationDefinition(id(location, slot), "stage", slot * 20.0D, 0.0D, 4.0D);
    }
    private static StoredMobSpawn spawn(int location, int slot, boolean enabled) {
        return new StoredMobSpawn(id(location, slot), slot, "stage-" + location + "-" + slot,
                slot * 20.0D, 64.0D, 0.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, enabled);
    }
}
