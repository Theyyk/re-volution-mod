package com.example.revolution.location.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MobZoneLayoutJUnitTest {
    @Test void oddPopulationRemainderBelongsToRandomArea() {
        assertEquals(0, MobZoneLayout.fixedCount(1));
        assertEquals(1, MobZoneLayout.randomCount(1));
        assertEquals(1, MobZoneLayout.fixedCount(3));
        assertEquals(2, MobZoneLayout.randomCount(3));
        assertEquals(2, MobZoneLayout.fixedCount(5));
        assertEquals(3, MobZoneLayout.randomCount(5));
        assertEquals(3, MobZoneLayout.fixedCount(6));
        assertEquals(3, MobZoneLayout.randomCount(6));
    }

    @Test void fixedPointsAreDistinctAndRemainInsideZone() {
        int fixedCount = 4;
        MobZoneLayout.HorizontalPoint[] points = new MobZoneLayout.HorizontalPoint[fixedCount];
        for (int i = 0; i < fixedCount; i++) {
            points[i] = MobZoneLayout.fixedPoint(i, fixedCount, 10.0D, -5.0D, 12.0D);
            double dx = points[i].getX() - 10.0D;
            double dz = points[i].getZ() + 5.0D;
            assertTrue(Math.sqrt(dx * dx + dz * dz) < 12.0D);
        }
        for (int i = 0; i < fixedCount; i++) {
            for (int j = 0; j < i; j++) {
                double dx = points[i].getX() - points[j].getX();
                double dz = points[i].getZ() - points[j].getZ();
                assertTrue(Math.sqrt(dx * dx + dz * dz) >= 4.0D);
            }
        }
    }
}
