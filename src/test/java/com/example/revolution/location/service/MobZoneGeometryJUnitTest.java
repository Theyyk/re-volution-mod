package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import org.junit.jupiter.api.Test;

public final class MobZoneGeometryJUnitTest {
    @Test
    void generatedNormalZoneMatchesLegacySetGeometryContract() {
        LocationDefinition zone = MobZoneGeometry.normal(1, 4, 12.25D, -8.75D, 16.0D);

        assertEquals(MobZoneIds.encode(1, 4), zone.getLocationId());
        assertEquals("MobZone 1:4", zone.getName());
        assertEquals(12.25D, zone.getCenterX(), 0.0D);
        assertEquals(-8.75D, zone.getCenterZ(), 0.0D);
        assertEquals(16.0D, zone.getRadius(), 0.0D);
        assertEquals(LocationKind.MOB_ZONE, zone.getKind());
        assertFalse(zone.hasTransitionPoint());
    }
}
