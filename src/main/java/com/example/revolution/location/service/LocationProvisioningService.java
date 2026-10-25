package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;

/** Application service that persists planned stage geometry. It intentionally does not mutate Minecraft blocks. */
public final class LocationProvisioningService {
    private final LocationRepository locations;
    private final LocationGenerationPlanner planner;
    private final LocationTemplate defaultTemplate;
    private final LocationStageTemplateRepository stageTemplates;
    private final StageTransitionTriggerRepository triggers;

    /** Compatibility constructor. Stage creation still requires a configured stage template. */
    public LocationProvisioningService(LocationRepository locations) {
        this(locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage(),
                new NoopStageTemplateRepository(), null);
    }

    public LocationProvisioningService(
            LocationRepository locations,
            LocationGenerationPlanner planner,
            LocationTemplate defaultTemplate) {
        this(locations, planner, defaultTemplate, new NoopStageTemplateRepository(), null);
    }

    public LocationProvisioningService(
            LocationRepository locations,
            LocationGenerationPlanner planner,
            LocationTemplate defaultTemplate,
            LocationStageTemplateRepository stageTemplates,
            StageTransitionTriggerRepository triggers) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (planner == null) throw new IllegalArgumentException("planner is required");
        if (defaultTemplate == null) throw new IllegalArgumentException("defaultTemplate is required");
        if (stageTemplates == null) throw new IllegalArgumentException("stageTemplates is required");
        this.locations = locations;
        this.planner = planner;
        this.defaultTemplate = defaultTemplate;
        this.stageTemplates = stageTemplates;
        this.triggers = triggers;
    }

    public LocationStageTemplate setStageTemplate(
            int worldLocationId, double mobRadius, double triggerOffset, double triggerRadius) {
        LocationStageTemplate template = new LocationStageTemplate(
                worldLocationId, mobRadius, triggerOffset, triggerRadius);
        stageTemplates.upsert(template);
        return template;
    }

    public LocationStageTemplate getStageTemplate(int worldLocationId) {
        return stageTemplates.get(worldLocationId);
    }

    public LocationGenerationPlan createStage(
            int worldLocationId,
            int mobSlotId,
            double anchorX,
            double anchorY,
            double anchorZ,
            long worldSeed,
            Long explicitSeed) {
        int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
        if (locations.getLocation(internalId) != null) {
            throw new IllegalArgumentException("Stage уже существует: location=" + worldLocationId + " slot=" + mobSlotId);
        }
        ensureNoStageAfterBoss(worldLocationId, mobSlotId);

        LocationStageTemplate saved = stageTemplates.get(worldLocationId);
        if (saved == null) {
            throw new IllegalArgumentException(
                    "Stage template is not configured for Location " + worldLocationId
                            + ". Use /location stagetemplate <locationId> <mobRadius> <triggerOffset> <triggerRadius> first.");
        }
        LocationTemplate generationTemplate = new LocationTemplate(
                "stage-template-" + worldLocationId,
                saved.getMobRadius(),
                defaultTemplate.getDecorationKinds());

        long seed = explicitSeed != null
                ? explicitSeed
                : planner.defaultSeed(worldSeed, worldLocationId, mobSlotId,
                        floorBlock(anchorX), floorBlock(anchorY), floorBlock(anchorZ));
        LocationGenerationPlan plan = planner.plan(
                generationTemplate, worldLocationId, mobSlotId, anchorX, anchorY, anchorZ, seed);
        LocationDefinition zone = MobZoneGeometry.normal(
                worldLocationId,
                mobSlotId,
                plan.getCenterX(),
                plan.getCenterZ(),
                plan.getRadius());
        locations.upsertLocation(zone);

        if (triggers != null) {
            try {
                triggers.upsert(new StageTransitionTriggerDefinition(
                        worldLocationId,
                        mobSlotId,
                        saved.getTriggerOffset(),
                        saved.getTriggerRadius(),
                        StageTransitionTriggerDefinition.DisplayMode.ACTION_BAR,
                        null,
                        null,
                        null));
            } catch (RuntimeException e) {
                locations.deleteLocation(internalId);
                throw e;
            }
        }
        return plan;
    }

    private void ensureNoStageAfterBoss(int worldLocationId, int proposedSlot) {
        for (LocationDefinition zone : locations.getLocations()) {
            if (!zone.isBossZone() || !MobZoneIds.isMobZoneId(zone.getLocationId())) continue;
            if (MobZoneIds.worldLocationId(zone.getLocationId()) != worldLocationId) continue;
            int bossSlot = MobZoneIds.mobSlotId(zone.getLocationId());
            if (proposedSlot > bossSlot) {
                throw new IllegalArgumentException("Нельзя создать stage после Boss Zone slot=" + bossSlot);
            }
        }
    }

    private static int floorBlock(double value) {
        return (int) Math.floor(value);
    }

    private static final class NoopStageTemplateRepository implements LocationStageTemplateRepository {
        @Override public LocationStageTemplate get(int worldLocationId) { return null; }
        @Override public void upsert(LocationStageTemplate template) { }
        @Override public void delete(int worldLocationId) { }
    }
}
