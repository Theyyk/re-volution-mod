package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;

/** Canonical geometry factory for admin-created mob stages. */
public final class MobZoneGeometry {
    private MobZoneGeometry() {}

    public static LocationDefinition normal(
            int worldLocationId,
            int mobSlotId,
            double centerX,
            double centerZ,
            double radius) {
        int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
        return new LocationDefinition(
                internalId,
                "MobZone " + worldLocationId + ":" + mobSlotId,
                centerX,
                centerZ,
                radius,
                LocationKind.MOB_ZONE);
    }
}
