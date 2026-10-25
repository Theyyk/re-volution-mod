package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.generation.LocationGenerationPlan;
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

public final class LocationStageTemplateProvisioningJUnitTest {
    @Test
    void savedTemplateControlsZoneAndAutomaticTrigger() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryTemplates templates = new MemoryTemplates();
        MemoryTriggers triggers = new MemoryTriggers();
        LocationProvisioningService service = new LocationProvisioningService(
                locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(), templates, triggers);

        service.setStageTemplate(1, 15.0D, 5.0D, 5.0D);
        LocationGenerationPlan plan = service.createStage(1, 2, 10.0D, 64.0D, 20.0D, 999L, 123L);

        LocationDefinition zone = locations.getLocation(MobZoneIds.encode(1, 2));
        assertNotNull(zone);
        assertEquals(15.0D, zone.getRadius(), 0.0D);
        assertEquals(15.0D, plan.getRadius(), 0.0D);
        assertEquals(123L, plan.getSeed());
        assertTrue(locations.getMobSpawns(zone.getLocationId()).isEmpty());

        StageTransitionTriggerDefinition trigger = triggers.get(1, 2);
        assertNotNull(trigger);
        assertEquals(5.0D, trigger.getOffset(), 0.0D);
        assertEquals(5.0D, trigger.getRadius(), 0.0D);
    }

    @Test
    void stageTemplateIsStoredAtWorldLocationLevel() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryTemplates templates = new MemoryTemplates();
        LocationProvisioningService service = new LocationProvisioningService(
                locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(), templates, new MemoryTriggers());

        service.setStageTemplate(7, 18.0D, 3.0D, 4.0D);
        LocationStageTemplate saved = service.getStageTemplate(7);

        assertNotNull(saved);
        assertEquals(7, saved.getWorldLocationId());
        assertEquals(18.0D, saved.getMobRadius(), 0.0D);
        assertEquals(3.0D, saved.getTriggerOffset(), 0.0D);
        assertEquals(4.0D, saved.getTriggerRadius(), 0.0D);
    }

    @Test
    void missingTemplateRejectsCreateWithoutMutatingRepositories() {
        InMemoryLocationRepository locations = new InMemoryLocationRepository();
        MemoryTriggers triggers = new MemoryTriggers();
        LocationProvisioningService service = new LocationProvisioningService(
                locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(), new MemoryTemplates(), triggers);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createStage(9, 1, 0.0D, 64.0D, 0.0D, 1L, 55L));

        assertTrue(error.getMessage().contains("Stage template is not configured for Location 9"));
        assertNull(locations.getLocation(MobZoneIds.encode(9, 1)));
        assertNull(triggers.get(9, 1));
        assertEquals(0, locations.locationWrites);
        assertEquals(0, triggers.writes);
    }

    private static final class MemoryTemplates implements LocationStageTemplateRepository {
        private final Map<Integer, LocationStageTemplate> values = new HashMap<>();
        @Override public LocationStageTemplate get(int worldLocationId) { return values.get(worldLocationId); }
        @Override public void upsert(LocationStageTemplate template) { values.put(template.getWorldLocationId(), template); }
        @Override public void delete(int worldLocationId) { values.remove(worldLocationId); }
    }

    private static final class MemoryTriggers implements StageTransitionTriggerRepository {
        private final Map<String, StageTransitionTriggerDefinition> values = new HashMap<>();
        private int writes;
        @Override public List<StageTransitionTriggerDefinition> getAll() { return new ArrayList<>(values.values()); }
        @Override public StageTransitionTriggerDefinition get(int worldLocationId, int sourceMobSlotId) {
            return values.get(worldLocationId + ":" + sourceMobSlotId);
        }
        @Override public void upsert(StageTransitionTriggerDefinition trigger) {
            values.put(trigger.key(), trigger);
            writes++;
        }
        @Override public void delete(int worldLocationId, int sourceMobSlotId) { values.remove(worldLocationId + ":" + sourceMobSlotId); }
    }
}
