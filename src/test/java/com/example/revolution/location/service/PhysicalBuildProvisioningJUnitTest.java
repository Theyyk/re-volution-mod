package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.location.definition.LocationStageTemplate;
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

final class PhysicalBuildProvisioningJUnitTest {
    private final InMemoryLocationRepository locations = new InMemoryLocationRepository();
    private final PhysicalBuildTestSupport builds = new PhysicalBuildTestSupport();
    private final Map<Integer, LocationStageTemplate> stages = new HashMap<>();
    private final Map<String, StageTransitionTriggerDefinition> triggerValues = new HashMap<>();
    private boolean failTrigger;
    private int triggerWrites;
    private final StageTransitionTriggerRepository triggers = new StageTransitionTriggerRepository() {
        @Override public List<StageTransitionTriggerDefinition> getAll() { return new ArrayList<>(triggerValues.values()); }
        @Override public StageTransitionTriggerDefinition get(int id, int slot) { return triggerValues.get(id + ":" + slot); }
        @Override public void upsert(StageTransitionTriggerDefinition value) {
            triggerWrites++;
            triggerValues.put(value.key(), value);
            if (failTrigger) { failTrigger = false; throw new IllegalStateException("Trigger write failed after accept"); }
        }
        @Override public void delete(int id, int slot) { triggerValues.remove(id + ":" + slot); }
    };
    private final LocationProvisioningService service = new LocationProvisioningService(
            locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(),
            new LocationStageTemplateRepository() {
                @Override public LocationStageTemplate get(int id) { return stages.get(id); }
                @Override public void upsert(LocationStageTemplate value) { stages.put(value.getWorldLocationId(), value); }
                @Override public void delete(int id) { stages.remove(id); }
            }, triggers, builds, builds, builds);

    @Test void bindingValidatesBeforeSaveAndDoesNotReplaceValidBindingOnError() {
        service.setBuildTemplate(1, "test_template");
        assertEquals(1, builds.templateWrites);
        for (String name : new String[] {"unknown", "../test_template", "test_template.rebuild", "", "UPPER"}) {
            assertThrows(IllegalArgumentException.class, () -> service.setBuildTemplate(1, name));
        }
        builds.valid = false;
        assertThrows(IllegalArgumentException.class, () -> service.setBuildTemplate(1, "test_template"));
        assertEquals("test_template", service.getBuildTemplate(1));
        assertEquals(1, builds.templateWrites);
    }

    @Test void missingStageTemplateRejectsBeforeWorldPreparationOrWrites() {
        service.setBuildTemplate(1, "test_template");
        assertTrue(assertThrows(IllegalArgumentException.class, this::create).getMessage().contains("stagetemplate"));
        assertUntouched();
    }

    @Test void missingBuildTemplateRejectsBeforeWorldPreparationOrWrites() {
        service.setStageTemplate(1, 15, 5, 4);
        assertTrue(assertThrows(IllegalArgumentException.class, this::create).getMessage().contains("buildtemplate"));
        assertUntouched();
    }

    @Test void removedOrInvalidBuildTemplateIsRevalidatedAtCreate() {
        configure();
        builds.valid = false;
        assertThrows(IllegalArgumentException.class, this::create);
        assertUntouched();
    }

    @Test void unavailableStorageRejectsBeforeWorldPreparation() {
        configure();
        builds.available = false;
        assertThrows(IllegalStateException.class, this::create);
        assertUntouched();
    }

    @Test void worldValidationFailureDoesNotWriteOrApply() {
        configure();
        builds.failPrepare = true;
        assertThrows(IllegalStateException.class, this::create);
        assertEquals(1, builds.prepares);
        assertEquals(0, builds.applies);
        assertEquals(0, locations.locationWrites);
        assertEquals(0, triggerWrites);
        assertTrue(builds.saved.isEmpty());
    }

    @Test void partiallyFailedBuildIsRolledBackBeforeAnyStageWrite() {
        configure();
        builds.failApply = true;
        assertThrows(IllegalStateException.class, this::create);
        assertEquals(0, locations.locationWrites);
        assertEquals(0, triggerWrites);
        assertRolledBack();
    }

    @Test void failedTriggerWriteRemovesAcceptedStageAndTriggerAndRestoresWorld() {
        configure();
        failTrigger = true;
        assertThrows(IllegalStateException.class, this::create);
        assertRolledBack();
        assertTrue(triggerValues.isEmpty());
    }

    @Test void metadataFailureRemovesAllAcceptedWritesAndRestoresPreviousTrigger() {
        configure();
        StageTransitionTriggerDefinition previous = new StageTransitionTriggerDefinition(
                1, 2, 99, 7, StageTransitionTriggerDefinition.DisplayMode.CHAT, null, null, null);
        triggerValues.put(previous.key(), previous);
        builds.failSave = true;
        assertThrows(IllegalStateException.class, this::create);
        assertRolledBack();
        assertSame(previous, triggers.get(1, 2));
    }

    @Test void failedDatabaseCompensationStillRestoresWorldAndReportsIncompleteRollback() {
        configure();
        builds.failSave = true;
        builds.failDelete = true;
        IllegalStateException failure = assertThrows(IllegalStateException.class, this::create);
        assertTrue(failure.getMessage().contains("rollback was incomplete"));
        assertEquals(1, builds.rollbacks);
        assertFalse(builds.changed);
        assertTrue(locations.getLocations().isEmpty());
        assertTrue(triggerValues.isEmpty());
    }

    @Test void successUsesBuildNameAnchorSeedAndNoDecorationPopulationOrTransitionPoint() {
        configure();
        LocationGenerationPlan plan = create();
        assertSame(plan, builds.prepared);
        assertSame(plan, builds.saved.get(plan.getInternalLocationId()));
        assertEquals("test_template", plan.getTemplateId());
        assertEquals(-12, plan.getCenterX());
        assertEquals(64, plan.getCenterY());
        assertEquals(27, plan.getCenterZ());
        assertEquals(123L, plan.getSeed());
        assertEquals(new LocationGenerationPlanner().rotationForSeed(123L), plan.getRotationDegrees());
        assertTrue(plan.getDecorations().isEmpty());
        assertTrue(locations.getMobSpawns().isEmpty());
        assertFalse(locations.getLocation(plan.getInternalLocationId()).isBossZone());
        assertFalse(locations.getLocation(plan.getInternalLocationId()).hasTransitionPoint());
        assertEquals(plan.getCenterX(), locations.getLocation(plan.getInternalLocationId()).getCenterX());
        assertNotNull(triggers.get(1, 2));
        assertEquals(1, builds.applies);
        assertEquals(0, builds.rollbacks);
    }

    @Test void defaultSeedUsesExistingWorldSlotAndBlockAnchorFoundation() {
        configure();
        LocationGenerationPlan plan = service.createStage(1, 2, -12, 64, 27, 999L, null, builds);
        assertEquals(new LocationGenerationPlanner().defaultSeed(999L, 1, 2, -12, 64, 27), plan.getSeed());
    }

    private void configure() {
        service.setStageTemplate(1, 15, 5, 4);
        service.setBuildTemplate(1, "test_template");
    }

    private LocationGenerationPlan create() { return service.createStage(1, 2, -12, 64, 27, 999L, 123L, builds); }

    private void assertUntouched() {
        assertEquals(0, locations.locationWrites);
        assertEquals(0, triggerWrites);
        assertEquals(0, builds.prepares);
        assertEquals(0, builds.applies);
        assertTrue(builds.saved.isEmpty());
    }

    private void assertRolledBack() {
        assertFalse(builds.changed);
        assertEquals(1, builds.rollbacks);
        assertTrue(locations.getLocations().isEmpty());
        assertTrue(builds.saved.isEmpty());
    }
}
