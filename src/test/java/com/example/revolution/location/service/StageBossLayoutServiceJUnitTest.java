package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StageBossLayoutServiceJUnitTest {
    @Test void finalConfiguredStageBecomesOnlyPersistedBoss() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        repository.seed(zone(1, 1, LocationKind.MOB_ZONE), zone(1, 2, LocationKind.MOB_ZONE), zone(1, 3, LocationKind.MOB_ZONE));

        StageBossLayoutService service = new StageBossLayoutService(repository);
        assertEquals(1, service.reconcileLocation(1));
        assertFalse(repository.getLocation(id(1, 1)).isBossZone());
        assertFalse(repository.getLocation(id(1, 2)).isBossZone());
        assertTrue(repository.getLocation(id(1, 3)).isBossZone());
        assertEquals(0, service.reconcileLocation(1));
    }

    @Test void staleIntermediateBossMovesToNewFinalStage() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        LocationDefinition staleBoss = new LocationDefinition(
                id(1, 2), "custom-name", 20.0D, 30.0D, 5.0D, LocationKind.BOSS_ZONE,
                21.0D, 64.0D, 31.0D, 90.0F, 5.0F);
        repository.seed(zone(1, 1, LocationKind.MOB_ZONE), staleBoss, zone(1, 3, LocationKind.MOB_ZONE));

        StageBossLayoutService service = new StageBossLayoutService(repository);
        assertEquals(2, service.reconcileLocation(1));

        LocationDefinition demoted = repository.getLocation(id(1, 2));
        assertFalse(demoted.isBossZone());
        assertEquals("custom-name", demoted.getName());
        assertEquals(20.0D, demoted.getCenterX());
        assertEquals(30.0D, demoted.getCenterZ());
        assertEquals(5.0D, demoted.getRadius());
        assertTrue(demoted.hasTransitionPoint());
        assertEquals(21.0D, demoted.getTransitionX());
        assertEquals(64.0D, demoted.getTransitionY());
        assertEquals(31.0D, demoted.getTransitionZ());
        assertEquals(90.0F, demoted.getTransitionYaw());
        assertEquals(5.0F, demoted.getTransitionPitch());
        assertTrue(repository.getLocation(id(1, 3)).isBossZone());
    }

    @Test void locationsAreNormalizedIndependently() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        repository.seed(
                zone(1, 1, LocationKind.MOB_ZONE), zone(1, 4, LocationKind.MOB_ZONE),
                zone(2, 2, LocationKind.BOSS_ZONE), zone(2, 7, LocationKind.MOB_ZONE));

        StageBossLayoutService service = new StageBossLayoutService(repository);
        assertEquals(3, service.reconcileAll());
        assertTrue(repository.getLocation(id(1, 4)).isBossZone());
        assertFalse(repository.getLocation(id(2, 2)).isBossZone());
        assertTrue(repository.getLocation(id(2, 7)).isBossZone());
    }

    private static int id(int location, int slot) { return MobZoneIds.encode(location, slot); }

    private static LocationDefinition zone(int location, int slot, LocationKind kind) {
        return new LocationDefinition(id(location, slot), "stage-" + location + "-" + slot,
                slot * 10.0D, location * 100.0D, 4.0D, kind);
    }
}
