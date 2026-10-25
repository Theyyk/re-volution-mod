package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition.DisplayMode;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import com.example.revolution.location.repository.StageTransitionPreferenceRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StageTransitionTriggerServiceJUnitTest {
    private InMemoryLocationRepository locations;
    private TriggerRepository repository;
    private MobProgressionService progression;
    private StageTransitionTriggerService service;

    @Test void newlyCommittedTriggerIsCachedWithoutRepositoryOrProgressionWrites() {
        int writes = repository.writes;
        service.rememberCreatedTrigger(1, 2,
                new com.example.revolution.location.definition.LocationStageTemplate(1, 5, 4, 6));
        assertEquals(writes, repository.writes);
        assertEquals(9D, service.overlayGeometry(1, 2).getInnerRadius());
        assertEquals(15D, service.overlayGeometry(1, 2).getOuterRadius());
    }

    @BeforeEach void setUp() {
        locations = new InMemoryLocationRepository();
        locations.seed(zone(2, 0.0D), zone(5, 20.0D), zone(9, 50.0D));
        repository = new TriggerRepository();
        progression = new MobProgressionService(new InMemoryMobProgressionRepository());
        progression.reloadConfiguration(locations.getLocations(), Arrays.asList(spawn(2), spawn(5), spawn(9)));
        service = new StageTransitionTriggerService(repository, new Preferences(), locations, progression,
                new StageTransitionService(locations, progression));
    }

    @Test void reconfigurationRetainsMessagesAndDisplayModeAndChangesOnlyRing() {
        service.configureTrigger(1, 2, 1.0D, 3.0D);
        service.setMessage(1, 2, "locked", "  locked  ");
        service.setMessage(1, 2, "forward", "forward");
        service.setMessage(1, 2, "backward", "backward");
        service.setDisplayMode(1, 2, DisplayMode.CHAT);
        StageTransitionTriggerDefinition updated = service.configureTrigger(1, 2, 4.0D, 6.0D);
        assertSame(updated, repository.get(1, 2));
        assertEquals("locked", updated.getLockedMessage());
        assertEquals("forward", updated.getForwardMessage());
        assertEquals("backward", updated.getBackwardMessage());
        assertEquals(DisplayMode.CHAT, updated.getDisplayMode());
        assertEquals(4.0D, updated.getOffset());
        assertEquals(6.0D, updated.getRadius());
        assertEquals(9.0D, service.overlayGeometry(1, 2).getInnerRadius());
        assertEquals(15.0D, service.overlayGeometry(1, 2).getOuterRadius());
    }

    @Test void clearingOneMessageRetainsOtherMessagesAndGeometry() {
        service.configureTrigger(1, 2, 1.0D, 3.0D);
        service.setMessage(1, 2, "locked", "locked");
        service.setMessage(1, 2, "forward", "forward");
        service.setMessage(1, 2, "backward", "backward");
        StageTransitionTriggerDefinition updated = service.clearMessage(1, 2, "forward");
        assertNull(updated.getForwardMessage());
        assertEquals("locked", updated.getLockedMessage());
        assertEquals("backward", updated.getBackwardMessage());
        assertEquals(1.0D, updated.getOffset());
        assertEquals(3.0D, updated.getRadius());
        assertNull(service.setMessage(1, 2, "locked", "   ").getLockedMessage());
    }

    @Test void invalidConfigurationAndMessageRequestsNeverWrite() {
        assertThrows(IllegalArgumentException.class, () -> service.configureTrigger(1, 99, 1.0D, 3.0D));
        assertThrows(IllegalArgumentException.class, () -> service.configureTrigger(1, 2, -1.0D, 3.0D));
        assertThrows(IllegalArgumentException.class, () -> service.configureTrigger(1, 2, 1.0D, Double.NaN));
        assertThrows(IllegalStateException.class, () -> service.setMessage(1, 2, "locked", "x"));
        assertEquals(0, repository.writes);
        service.configureTrigger(1, 2, 1.0D, 3.0D);
        assertThrows(IllegalArgumentException.class, () -> service.setMessage(1, 2, "typo", "x"));
        assertEquals(1, repository.writes);
        assertNull(repository.get(1, 2).getLockedMessage());
    }

    @Test void failedPersistenceDoesNotReplaceCachedTrigger() {
        service.configureTrigger(1, 2, 1.0D, 3.0D);
        repository.failWrite = true;
        assertThrows(IllegalStateException.class, () -> service.configureTrigger(1, 2, 7.0D, 9.0D));
        assertEquals(6.0D, service.overlayGeometry(1, 2).getInnerRadius());
        repository.failWrite = false;
        service.setMessage(1, 2, "locked", "after failure");
        assertEquals(1.0D, repository.get(1, 2).getOffset());
        assertEquals("after failure", repository.get(1, 2).getLockedMessage());
    }

    @Test void clearAndReloadUseRepositoryWithoutRetainingDeletedTrigger() {
        service.configureTrigger(1, 2, 1.0D, 3.0D);
        service.configureTrigger(1, 5, 2.0D, 4.0D);
        service.clearTrigger(1, 2);
        assertNull(repository.get(1, 2));
        assertNull(service.overlayGeometry(1, 2));
        assertNotNull(service.overlayGeometry(1, 5));
        repository.delete(1, 5);
        repository.upsert(new StageTransitionTriggerDefinition(1, 9, 3.0D, 5.0D,
                DisplayMode.CHAT, null, null, null));
        service.reload();
        assertNull(service.overlayGeometry(1, 5));
        assertEquals(8.0D, service.overlayGeometry(1, 9).getInnerRadius());
    }

    @Test void finalStageForwardAxisPointsAwayFromPreviousStage() {
        service.configureTrigger(1, 9, 1.0D, 3.0D);
        StageTransitionTriggerService.OverlayGeometry geometry = service.overlayGeometry(1, 9);
        assertEquals(30.0D, geometry.getForwardX());
        assertEquals(0.0D, geometry.getForwardZ());
        service.configureTrigger(1, 5, 1.0D, 3.0D);
        assertEquals(30.0D, service.overlayGeometry(1, 5).getForwardX());
    }

    @Test void absentOrCoincidentAdjacentGeometryHasNoInventedDirection() {
        locations.seed(zone(2, 0.0D), zone(5, 0.0D), zone(9, 0.0D));
        service.configureTrigger(1, 5, 1.0D, 3.0D);
        assertEquals(0.0D, service.overlayGeometry(1, 5).getForwardX());
        assertEquals(0.0D, service.overlayGeometry(1, 5).getForwardZ());
        locations.deleteLocation(MobZoneIds.encode(1, 5));
        assertNull(service.overlayGeometry(1, 5));
    }

    private static LocationDefinition zone(int slot, double x) {
        return new LocationDefinition(MobZoneIds.encode(1, slot), "stage", x, 0.0D, 5.0D);
    }
    private static StoredMobSpawn spawn(int slot) {
        return new StoredMobSpawn(MobZoneIds.encode(1, slot), slot, "mob", 0.0D, 64.0D, 0.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, true);
    }
    private static final class TriggerRepository implements StageTransitionTriggerRepository {
        private final Map<String, StageTransitionTriggerDefinition> values = new HashMap<>();
        private boolean failWrite;
        private int writes;
        @Override public List<StageTransitionTriggerDefinition> getAll() { return new ArrayList<>(values.values()); }
        @Override public StageTransitionTriggerDefinition get(int location, int slot) { return values.get(location + ":" + slot); }
        @Override public void upsert(StageTransitionTriggerDefinition value) {
            if (failWrite) throw new IllegalStateException("injected storage failure");
            writes++;
            values.put(value.key(), value);
        }
        @Override public void delete(int location, int slot) { values.remove(location + ":" + slot); }
    }
    private static final class Preferences implements StageTransitionPreferenceRepository {
        private final Map<UUID, Boolean> values = new HashMap<>();
        @Override public boolean isAutoTransitionEnabled(UUID player) { return values.getOrDefault(player, true); }
        @Override public void setAutoTransitionEnabled(UUID player, boolean enabled) { values.put(player, enabled); }
    }
}
