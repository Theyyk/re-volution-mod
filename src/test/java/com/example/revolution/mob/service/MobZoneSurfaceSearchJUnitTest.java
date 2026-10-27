package com.example.revolution.mob.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.LocationKind;
import com.example.revolution.location.definition.MobZoneIds;
import org.junit.jupiter.api.Test;

public final class MobZoneSurfaceSearchJUnitTest {
    @Test
    void findsOneBlockWideValidStripThatSparseRadialSamplesCanMiss() {
        LocationDefinition zone = new LocationDefinition(MobZoneIds.encode(1, 1), "MobZone test", 0.0D, 0.0D, 8.0D, LocationKind.MOB_ZONE);

        double[] result = MobZoneSurfaceSearch.findNearestBlockCenter(zone, 3.2D, 3.2D,
                new MobZoneSurfaceSearch.SurfaceProbe() {
                    @Override
                    public Double surfaceY(double x, double z) {
                        boolean onThinStrip = Math.abs(z - 0.5D) < 0.0001D && x >= -4.5D && x <= 4.5D;
                        return onThinStrip ? 64.0D : null;
                    }
                });

        assertNotNull(result);
        assertEquals(3.5D, result[0], 0.0001D);
        assertEquals(64.0D, result[1], 0.0001D);
        assertEquals(0.5D, result[2], 0.0001D);
    }

    @Test
    void doesNotInventSurfaceWhenSafetyProbeRejectsEveryBlock() {
        LocationDefinition zone = new LocationDefinition(MobZoneIds.encode(1, 1), "MobZone test", 0.0D, 0.0D, 5.0D, LocationKind.MOB_ZONE);

        double[] result = MobZoneSurfaceSearch.findNearestBlockCenter(zone, 0.0D, 0.0D,
                new MobZoneSurfaceSearch.SurfaceProbe() {
                    @Override
                    public Double surfaceY(double x, double z) {
                        return null;
                    }
                });

        assertNull(result);
    }
}
