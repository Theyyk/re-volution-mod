package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationStageTemplate;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StageTransitionTriggerDefinition;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.generation.BuildTemplateCatalog;
import com.example.revolution.location.generation.PhysicalStageBuilder;
import com.example.revolution.location.repository.LocationBuildTemplateRepository;
import com.example.revolution.location.repository.LocationGenerationRepository;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.repository.LocationStageTemplateRepository;
import com.example.revolution.location.repository.StageTransitionTriggerRepository;

/** Validates and plans first, then coordinates a physical build and compensated stage writes. */
public final class LocationProvisioningService {
    private final LocationRepository locations;
    private final LocationGenerationPlanner planner;
    private final LocationStageTemplateRepository stageTemplates;
    private final StageTransitionTriggerRepository triggers;
    private final LocationBuildTemplateRepository buildTemplates;
    private final BuildTemplateCatalog catalog;
    private final LocationGenerationRepository generations;

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
        this(locations, planner, defaultTemplate, stageTemplates, triggers,
                new LocationBuildTemplateRepository() {
                    @Override public String get(int id) { return null; }
                    @Override public void upsert(int id, String name) {
                        throw new IllegalStateException("Build template storage is unavailable");
                    }
                }, name -> { throw new IllegalStateException("REBuilder is unavailable"); }, null);
    }

    public LocationProvisioningService(
            LocationRepository locations, LocationGenerationPlanner planner, LocationTemplate defaultTemplate,
            LocationStageTemplateRepository stageTemplates, StageTransitionTriggerRepository triggers,
            LocationBuildTemplateRepository buildTemplates, BuildTemplateCatalog catalog,
            LocationGenerationRepository generations) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (planner == null) throw new IllegalArgumentException("planner is required");
        if (defaultTemplate == null) throw new IllegalArgumentException("defaultTemplate is required");
        if (stageTemplates == null) throw new IllegalArgumentException("stageTemplates is required");
        if (buildTemplates == null || catalog == null) throw new IllegalArgumentException("build templates and catalog are required");
        this.locations = locations;
        this.planner = planner;
        this.stageTemplates = stageTemplates;
        this.triggers = triggers;
        this.buildTemplates = buildTemplates;
        this.catalog = catalog;
        this.generations = generations;
    }

    public void setBuildTemplate(int worldLocationId, String templateName) {
        MobZoneIds.encode(worldLocationId, 1);
        if (templateName == null || !templateName.matches("[a-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid REBuilder template name; use the name from /rebuilder list without .rebuild");
        }
        catalog.validate(templateName);
        buildTemplates.upsert(worldLocationId, templateName);
    }

    public String getBuildTemplate(int worldLocationId) { return buildTemplates.get(worldLocationId); }

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
        return createStage(worldLocationId, mobSlotId, anchorX, anchorY, anchorZ, worldSeed, explicitSeed,
                plan -> { throw new IllegalStateException("Physical build requires a player/world adapter"); });
    }

    public LocationGenerationPlan createStage(
            int worldLocationId, int mobSlotId, double anchorX, double anchorY, double anchorZ,
            long worldSeed, Long explicitSeed, PhysicalStageBuilder builder) {
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
        String buildTemplate = buildTemplates.get(worldLocationId);
        if (buildTemplate == null) {
            throw new IllegalArgumentException("Build template is not configured for Location " + worldLocationId
                    + ". Use /location buildtemplate <locationId> <templateName> first.");
        }
        catalog.validate(buildTemplate);
        LocationTemplate generationTemplate = new LocationTemplate(
                buildTemplate, saved.getMobRadius(), java.util.Collections.emptyList());

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
        StageTransitionTriggerDefinition previousTrigger = triggers == null ? null : triggers.get(worldLocationId, mobSlotId);
        if (generations == null) throw new IllegalStateException("Generation storage is unavailable");
        generations.requireAvailable();
        if (builder == null) throw new IllegalArgumentException("Physical builder is required");
        PhysicalStageBuilder.PreparedBuild prepared = builder.prepare(plan);
        boolean zoneAttempted = false;
        boolean triggerAttempted = false;
        boolean generationAttempted = false;
        try {
            prepared.apply();
            zoneAttempted = true;
            locations.upsertLocation(zone);
            if (triggers != null) {
                triggerAttempted = true;
                triggers.upsert(new StageTransitionTriggerDefinition(
                        worldLocationId,
                        mobSlotId,
                        saved.getTriggerOffset(),
                        saved.getTriggerRadius(),
                        StageTransitionTriggerDefinition.DisplayMode.ACTION_BAR,
                        null,
                        null,
                        null));
            }
            generationAttempted = true;
            generations.save(plan);
        } catch (RuntimeException failure) {
            // Each cleanup is independent: a failed database compensation must not skip world restoration.
            if (generationAttempted) compensate(failure, () -> generations.delete(internalId));
            if (triggerAttempted) compensate(failure, () -> {
                if (previousTrigger == null) triggers.delete(worldLocationId, mobSlotId);
                else triggers.upsert(previousTrigger);
            });
            if (zoneAttempted) compensate(failure, () -> locations.deleteLocation(internalId));
            compensate(failure, prepared::rollback);
            if (failure.getSuppressed().length > 0) {
                throw new IllegalStateException("Location creation failed; rollback was incomplete. Check server log before retrying.", failure);
            }
            throw new IllegalStateException("Location creation failed: " + failure.getMessage(), failure);
        }
        return plan;
    }

    private static void compensate(RuntimeException failure, Runnable action) {
        try { action.run(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
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
