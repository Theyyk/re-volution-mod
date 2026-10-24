package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.FastAutoPreferenceRepository;
import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FastAutoServiceJUnitTest {
    private final UUID player = UUID.fromString("00000000-0000-0000-0000-000000001311");
    private InMemoryMobProgressionRepository progressionRepository;
    private MobProgressionService progression;
    private MemoryPreferences preferences;
    private FastAutoService fastAuto;

    @BeforeEach void setUp() {
        progressionRepository = new InMemoryMobProgressionRepository();
        progression = new MobProgressionService(progressionRepository);
        progression.reloadConfiguration(
                Arrays.asList(zone(1, 1), zone(1, 2), zone(2, 1), zone(2, 2)),
                Arrays.asList(spawn(1, 1), spawn(1, 2), spawn(2, 1), spawn(2, 2)));
        preferences = new MemoryPreferences();
        fastAuto = new FastAutoService(preferences, progression);
        progression.setUnlockListener(fastAuto);
    }

    @Test void fastAutoIsIndependentAndOffByDefault() {
        assertFalse(fastAuto.isEnabled(player));
        assertNotNull(progression.unlockNext(player, id(1, 1)));
        assertNull(fastAuto.pollPendingTarget(player));
    }

    @Test void enabledFastAutoQueuesExactlyUnlockedStage() {
        fastAuto.setEnabled(player, true);
        assertNotNull(progression.unlockNext(player, id(1, 1)));
        assertEquals(Integer.valueOf(id(1, 2)), fastAuto.pollPendingTarget(player));
        assertNull(fastAuto.pollPendingTarget(player));
    }

    @Test void bossUnlockQueuesFirstStageOfNextLocation() {
        fastAuto.setEnabled(player, true);
        assertNotNull(progression.unlockNext(player, id(1, 1)));
        assertEquals(Integer.valueOf(id(1, 2)), fastAuto.pollPendingTarget(player));
        assertNotNull(progression.unlockNext(player, id(1, 2)));
        assertEquals(Integer.valueOf(id(2, 1)), fastAuto.pollPendingTarget(player));
    }

    @Test void disablingFastAutoClearsPendingTarget() {
        fastAuto.setEnabled(player, true);
        assertNotNull(progression.unlockNext(player, id(1, 1)));
        fastAuto.setEnabled(player, false);
        assertNull(fastAuto.pollPendingTarget(player));
    }

    @Test void revokeInvalidatesQueuedTargetBeforeMovement() {
        fastAuto.setEnabled(player, true);
        assertNotNull(progression.unlockNext(player, id(1, 1)));
        assertTrue(progression.revokeFrom(player, 1, 2));
        assertNull(fastAuto.pollPendingTarget(player));
    }

    @Test void preferenceSurvivesServiceRecreationThroughRepository() {
        fastAuto.setEnabled(player, true);
        FastAutoService recreated = new FastAutoService(preferences, progression);
        assertTrue(recreated.isEnabled(player));
    }

    private static int id(int location, int slot) { return MobZoneIds.encode(location, slot); }
    private static LocationDefinition zone(int location, int slot) {
        return new LocationDefinition(id(location, slot), "stage", slot * 10.0D, location * 100.0D, 4.0D);
    }
    private static StoredMobSpawn spawn(int location, int slot) {
        return new StoredMobSpawn(id(location, slot), slot, "mob-" + location + "-" + slot,
                slot * 10.0D, 64.0D, location * 100.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, true);
    }

    private static final class MemoryPreferences implements FastAutoPreferenceRepository {
        private final Map<UUID, Boolean> values = new HashMap<>();
        @Override public boolean isFastAutoEnabled(UUID playerUuid) { return Boolean.TRUE.equals(values.get(playerUuid)); }
        @Override public void setFastAutoEnabled(UUID playerUuid, boolean enabled) { values.put(playerUuid, enabled); }
    }
}
