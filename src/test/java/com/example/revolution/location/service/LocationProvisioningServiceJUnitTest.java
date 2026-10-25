package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import org.junit.jupiter.api.Test;

public final class LocationProvisioningServiceJUnitTest {
    @Test
    void createStagePersistsEmptyMobZoneGeometry() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        LocationProvisioningService service = new LocationProvisioningService(repository);

        LocationGenerationPlan plan = service.createStage(1, 4, 12.5D, 70.0D, -8.5D, 99L, 123L);
        LocationDefinition saved = repository.getLocation(MobZoneIds.encode(1, 4));

        assertNotNull(saved);
        assertEquals(LocationKind.MOB_ZONE, saved.getKind());
        assertEquals(12.5D, saved.getCenterX(), 0.0D);
        assertEquals(-8.5D, saved.getCenterZ(), 0.0D);
        assertEquals(30.0D, saved.getRadius(), 0.0D);
        assertEquals(123L, plan.getSeed());
        assertTrue(repository.getMobSpawns(saved.getLocationId()).isEmpty());
        assertEquals(1, repository.locationWrites);
    }

    @Test
    void duplicateStageIsRejectedWithoutSecondWrite() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        LocationProvisioningService service = new LocationProvisioningService(repository);
        service.createStage(2, 1, 0.0D, 64.0D, 0.0D, 1L, 10L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createStage(2, 1, 5.0D, 64.0D, 5.0D, 1L, 20L));
        assertTrue(error.getMessage().contains("Stage уже существует"));
        assertEquals(1, repository.locationWrites);
    }

    @Test
    void stageAfterBossIsRejected() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        repository.seed(new LocationDefinition(
                MobZoneIds.encode(3, 3), "Boss", 0.0D, 0.0D, 30.0D, LocationKind.BOSS_ZONE));
        LocationProvisioningService service = new LocationProvisioningService(repository);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createStage(3, 4, 0.0D, 64.0D, 0.0D, 1L, null));
        assertTrue(error.getMessage().contains("после Boss Zone"));
        assertEquals(0, repository.locationWrites);
    }

    @Test
    void stageBeforeExistingBossRemainsAllowed() {
        InMemoryLocationRepository repository = new InMemoryLocationRepository();
        repository.seed(new LocationDefinition(
                MobZoneIds.encode(4, 5), "Boss", 0.0D, 0.0D, 30.0D, LocationKind.BOSS_ZONE));
        LocationProvisioningService service = new LocationProvisioningService(repository);

        service.createStage(4, 4, 25.0D, 64.0D, 25.0D, 1L, 77L);
        assertNotNull(repository.getLocation(MobZoneIds.encode(4, 4)));
        assertEquals(1, repository.locationWrites);
    }
}
