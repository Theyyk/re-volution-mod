package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.LocationRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persists the invariant that the final configured stage of each Location is the only Boss Zone. */
public final class StageBossLayoutService {
    private final LocationRepository locations;

    public StageBossLayoutService(LocationRepository locations) {
        if (locations == null) throw new IllegalArgumentException("locations is required");
        this.locations = locations;
    }

    public int reconcileAll() {
        Set<Integer> worldLocations = new LinkedHashSet<>();
        for (LocationDefinition zone : locations.getLocations()) {
            if (zone == null || !MobZoneIds.isMobZoneId(zone.getLocationId())) continue;
            worldLocations.add(MobZoneIds.worldLocationId(zone.getLocationId()));
        }
        int changed = 0;
        for (Integer worldLocationId : worldLocations) changed += reconcileLocation(worldLocationId);
        return changed;
    }

    public int reconcileLocation(int worldLocationId) {
        MobZoneIds.encode(worldLocationId, 1);
        List<LocationDefinition> zones = new ArrayList<>();
        int finalSlot = Integer.MIN_VALUE;
        for (LocationDefinition zone : locations.getLocations()) {
            if (zone == null || !MobZoneIds.isMobZoneId(zone.getLocationId())) continue;
            if (MobZoneIds.worldLocationId(zone.getLocationId()) != worldLocationId) continue;
            zones.add(zone);
            finalSlot = Math.max(finalSlot, MobZoneIds.mobSlotId(zone.getLocationId()));
        }
        if (zones.isEmpty()) return 0;

        List<LocationDefinition> changedBefore = new ArrayList<>();
        try {
            for (LocationDefinition zone : zones) {
                boolean shouldBeBoss = MobZoneIds.mobSlotId(zone.getLocationId()) == finalSlot;
                LocationKind desired = shouldBeBoss ? LocationKind.BOSS_ZONE : LocationKind.MOB_ZONE;
                if (zone.getKind() == desired) continue;
                changedBefore.add(zone);
                locations.upsertLocation(withKind(zone, desired));
            }
            return changedBefore.size();
        } catch (RuntimeException failure) {
            for (int i = changedBefore.size() - 1; i >= 0; i--) {
                LocationDefinition original = changedBefore.get(i);
                try { locations.upsertLocation(original); }
                catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    private static LocationDefinition withKind(LocationDefinition source, LocationKind kind) {
        if (source.hasTransitionPoint()) {
            return new LocationDefinition(
                    source.getLocationId(), source.getName(), source.getCenterX(), source.getCenterZ(), source.getRadius(), kind,
                    source.getTransitionX(), source.getTransitionY(), source.getTransitionZ(),
                    source.getTransitionYaw(), source.getTransitionPitch());
        }
        return new LocationDefinition(
                source.getLocationId(), source.getName(), source.getCenterX(), source.getCenterZ(), source.getRadius(), kind);
    }
}
