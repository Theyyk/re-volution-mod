package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.LocationGenerationPlanner;
import com.example.revolution.location.generation.LocationTemplate;
import com.example.revolution.location.repository.LocationRepository;

/** Application service that persists planned stage geometry. It intentionally does not mutate Minecraft blocks. */
public final class LocationProvisioningService {
    private final LocationRepository locations;
    private final LocationGenerationPlanner planner;
    private final LocationTemplate defaultTemplate;

    public LocationProvisioningService(LocationRepository locations) {
        this(locations, new LocationGenerationPlanner(), LocationTemplate.defaultStage());
    }

    public LocationProvisioningService(
            LocationRepository locations,
            LocationGenerationPlanner planner,
            LocationTemplate defaultTemplate) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (planner == null) throw new IllegalArgumentException("planner is required");
        if (defaultTemplate == null) throw new IllegalArgumentException("defaultTemplate is required");
        this.locations = locations;
        this.planner = planner;
        this.defaultTemplate = defaultTemplate;
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

        long seed = explicitSeed != null
                ? explicitSeed
                : planner.defaultSeed(worldSeed, worldLocationId, mobSlotId,
                        floorBlock(anchorX), floorBlock(anchorY), floorBlock(anchorZ));
        LocationGenerationPlan plan = planner.plan(
                defaultTemplate, worldLocationId, mobSlotId, anchorX, anchorY, anchorZ, seed);
        LocationDefinition zone = new LocationDefinition(
                internalId,
                "MobZone " + worldLocationId + ":" + mobSlotId,
                plan.getCenterX(),
                plan.getCenterZ(),
                plan.getRadius(),
                LocationKind.MOB_ZONE);
        locations.upsertLocation(zone);
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
}
