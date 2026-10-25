package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.repository.InMemoryLocationRepository;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AutoTransitionPointProvisioningJUnitTest {
    private final InMemoryLocationRepository locations = new InMemoryLocationRepository();
    private final PhysicalBuildTestSupport builds = new PhysicalBuildTestSupport();
    private final Map<Integer, LocationStageTemplate> stageTemplates = new HashMap<>();
    private final Map<String, StageTransitionTriggerDefinition> triggerValues = new HashMap<>();
    private final StageTransitionTriggerRepository triggers = new StageTransitionTriggerRepository() {
        @Override public List<StageTransitionTriggerDefinition> getAll() { return new ArrayList<>(triggerValues.values()); }
        @Override public StageTransitionTriggerDefinition get(int id, int slot) { return triggerValues.get(id + ":" + slot); }
        @Override public void upsert(StageTransitionTriggerDefinition value) { triggerValues.put(value.key(), value); }
        @Override public void delete(int id, int slot) { triggerValues.remove(id + ":" + slot); }
    };
    private final LocationProvisioningService service = new LocationProvisioningService(
            locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(),
            new LocationStageTemplateRepository() {
                @Override public LocationStageTemplate get(int id) { return stageTemplates.get(id); }
                @Override public void upsert(LocationStageTemplate value) { stageTemplates.put(value.getWorldLocationId(), value); }
                @Override public void delete(int id) { stageTemplates.remove(id); }
            }, triggers, builds, builds, builds);

    @Test void createStoresAutomaticPointBetweenCenterAndBackwardTrigger() {
        configure(1);
        locations.seed(MobZoneGeometry.normal(1, 1, -40.0D, 0.0D, 30.0D));

        service.createStage(1, 2, 0.0D, 64.0D, 0.0D, 999L, 123L, builds);

        LocationDefinition created = locations.getLocation(MobZoneIds.encode(1, 2));
        assertNotNull(created);
        assertTrue(created.hasTransitionPoint());
        assertEquals(-17.5D, created.getTransitionX(), 1.0E-9D);
        assertEquals(64.0D, created.getTransitionY(), 1.0E-9D);
        assertEquals(0.0D, created.getTransitionZ(), 1.0E-9D);
        assertEquals(-90.0F, created.getTransitionYaw(), 1.0E-5F);
    }

    @Test void createUsesExistingNextStageBeforePreviousForBackwardHalfAxis() {
        configure(1);
        locations.seed(
                MobZoneGeometry.normal(1, 1, -40.0D, 0.0D, 30.0D),
                MobZoneGeometry.normal(1, 3, 0.0D, 50.0D, 30.0D));

        service.createStage(1, 2, 0.0D, 64.0D, 0.0D, 999L, 123L, builds);

        LocationDefinition created = locations.getLocation(MobZoneIds.encode(1, 2));
        assertTrue(created.hasTransitionPoint());
        assertEquals(0.0D, created.getTransitionX(), 1.0E-9D);
        assertEquals(-17.5D, created.getTransitionZ(), 1.0E-9D);
    }

    @Test void firstStageWithoutAnyAdjacentTopologyRemainsWithoutInventedPoint() {
        configure(1);

        service.createStage(1, 1, 0.0D, 64.0D, 0.0D, 999L, 123L, builds);

        assertFalse(locations.getLocation(MobZoneIds.encode(1, 1)).hasTransitionPoint());
    }

    @Test void firstStageOfNextLocationUsesFinalBossAsBackwardNeighbor() {
        configure(2);
        int bossId = MobZoneIds.encode(1, 3);
        locations.seed(
                MobZoneGeometry.normal(1, 1, -80.0D, 0.0D, 30.0D),
                MobZoneGeometry.normal(1, 2, -60.0D, 0.0D, 30.0D),
                new LocationDefinition(bossId, "Boss 1-3", -40.0D, 0.0D, 30.0D, LocationKind.BOSS_ZONE));

        service.createStage(2, 1, 0.0D, 64.0D, 0.0D, 999L, 123L, builds);

        LocationDefinition created = locations.getLocation(MobZoneIds.encode(2, 1));
        assertTrue(created.hasTransitionPoint());
        assertEquals(-17.5D, created.getTransitionX(), 1.0E-9D);
        assertEquals(0.0D, created.getTransitionZ(), 1.0E-9D);
    }

    @Test void invalidPreviousLocationBossLayoutDoesNotInventCrossLocationPoint() {
        configure(2);
        locations.seed(
                MobZoneGeometry.normal(1, 1, -60.0D, 0.0D, 30.0D),
                MobZoneGeometry.normal(1, 2, -40.0D, 0.0D, 30.0D));

        service.createStage(2, 1, 0.0D, 64.0D, 0.0D, 999L, 123L, builds);

        assertFalse(locations.getLocation(MobZoneIds.encode(2, 1)).hasTransitionPoint());
    }

    private void configure(int worldLocationId) {
        service.setStageTemplate(worldLocationId, 30.0D, 5.0D, 5.0D);
        service.setBuildTemplate(worldLocationId, "test_template");
    }
}
