package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StageTransitionServiceJUnitTest {
    private InMemoryLocationRepository repository;
    private StageTransitionService service;

    @BeforeEach void setUp() {
        repository = new InMemoryLocationRepository();
        repository.seed(new LocationDefinition(MobZoneIds.encode(1, 2), "original", 10.0D, -5.0D, 3.0D));
        service = new StageTransitionService(repository,
                new MobProgressionService(new InMemoryMobProgressionRepository()));
    }

    @Test void configuringAndClearingDestinationPreservesSourceGeometryInRepository() {
        LocationDefinition updated = service.configureTransitionPoint(1, 2, 5000.0D, 75.0D, -9000.0D, 45.0F, 12.0F);
        assertSame(updated, repository.getLocation(MobZoneIds.encode(1, 2)));
        assertTrue(updated.hasTransitionPoint());
        assertEquals(5000.0D, updated.getTransitionX());
        LocationDefinition cleared = service.clearTransitionPoint(1, 2);
        assertSame(cleared, repository.getLocation(MobZoneIds.encode(1, 2)));
        assertFalse(cleared.hasTransitionPoint());
        for (LocationDefinition value : Arrays.asList(updated, cleared)) {
            assertEquals("original", value.getName());
            assertEquals(10.0D, value.getCenterX());
            assertEquals(-5.0D, value.getCenterZ());
            assertEquals(3.0D, value.getRadius());
        }
        assertEquals(2, repository.locationWrites);
    }

    @Test void invalidCoordinatesAndUnknownStagesDoNotWrite() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.configureTransitionPoint(1, 2, invalid, 64.0D, 0.0D, 0.0F, 0.0F));
            assertThrows(IllegalArgumentException.class,
                    () -> service.configureTransitionPoint(1, 2, 0.0D, invalid, 0.0D, 0.0F, 0.0F));
            assertThrows(IllegalArgumentException.class,
                    () -> service.configureTransitionPoint(1, 2, 0.0D, 64.0D, invalid, 0.0F, 0.0F));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.configureTransitionPoint(1, 2, 0.0D, 64.0D, 0.0D, Float.NaN, 0.0F));
        assertThrows(IllegalArgumentException.class,
                () -> service.configureTransitionPoint(1, 2, 0.0D, 64.0D, 0.0D, 0.0F, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> service.configureTransitionPoint(9, 9, 0.0D, 64.0D, 0.0D, 0.0F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> service.clearTransitionPoint(9, 9));
        assertEquals(0, repository.locationWrites);
        assertFalse(repository.getLocation(MobZoneIds.encode(1, 2)).hasTransitionPoint());
    }

    @Test void overlappingZonesFailInsteadOfDependingOnRepositoryOrder() {
        repository.seed(new LocationDefinition(MobZoneIds.encode(1, 5), "overlap", 10.0D, -5.0D, 3.0D));
        assertThrows(IllegalStateException.class, () -> service.findCurrentMobZone(10.0D, -5.0D));
    }

    @Test void legacyGeometryDoesNotBecomeMobStageContext() {
        repository.seed(new LocationDefinition(1, "legacy", 100.0D, 100.0D, 50.0D));
        assertNull(service.findCurrentMobZone(100.0D, 100.0D));
        assertEquals(MobZoneIds.encode(1, 2), service.findCurrentMobZone(10.0D, -5.0D).getLocationId());
    }

    @Test void nearestTiesUseWorldLocationThenSlotRegardlessOfInputOrder() {
        LocationDefinition laterLocation = point(2, 1, -1.0D);
        LocationDefinition laterSlot = point(1, 9, 1.0D);
        LocationDefinition winner = point(1, 2, -1.0D);
        assertSame(winner, StageTransitionService.nearestConfiguredStage(
                Arrays.asList(laterLocation, laterSlot, winner), 0.0D, 0.0D));
        assertSame(winner, StageTransitionService.nearestConfiguredStage(
                Arrays.asList(winner, laterSlot, laterLocation), 0.0D, 0.0D));
    }

    @Test void nearestIgnoresNullLegacyAndUnconfiguredEntries() {
        LocationDefinition configured = point(1, 5, 100.0D);
        LocationDefinition unconfigured = repository.getLocation(MobZoneIds.encode(1, 2));
        LocationDefinition legacy = new LocationDefinition(1, "legacy", 0.0D, 0.0D, 1.0D,
                0.0D, 64.0D, 0.0D, 0.0F, 0.0F);
        assertSame(configured, StageTransitionService.nearestConfiguredStage(
                Arrays.asList(null, legacy, unconfigured, configured), 0.0D, 0.0D));
        assertNull(StageTransitionService.nearestConfiguredStage(Arrays.asList(null, legacy, unconfigured), 0.0D, 0.0D));
        assertNull(StageTransitionService.nearestConfiguredStage(null, 0.0D, 0.0D));
    }

    private static LocationDefinition point(int location, int slot, double x) {
        return new LocationDefinition(MobZoneIds.encode(location, slot), "point", 50.0D, 50.0D, 1.0D,
                x, 64.0D, 0.0D, 0.0F, 0.0F);
    }
}
