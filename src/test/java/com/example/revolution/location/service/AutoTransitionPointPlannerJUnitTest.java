package com.example.revolution.location.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.location.definition.LocationDefinition;
import org.junit.jupiter.api.Test;

final class AutoTransitionPointPlannerJUnitTest {
    private final AutoTransitionPointPlanner planner = new AutoTransitionPointPlanner();

    @Test void previousOnlyPlacesPointHalfwayTowardBackwardTriggerAndFacesCenter() {
        LocationDefinition source = MobZoneGeometry.normal(1, 2, 0.0D, 0.0D, 30.0D);
        LocationDefinition previous = MobZoneGeometry.normal(1, 1, -40.0D, 0.0D, 30.0D);

        AutoTransitionPointPlanner.Point point = planner.plan(source, previous, null, 5.0D, 64.0D);

        assertNotNull(point);
        assertEquals(-17.5D, point.getX(), 1.0E-9D);
        assertEquals(64.0D, point.getY(), 1.0E-9D);
        assertEquals(0.0D, point.getZ(), 1.0E-9D);
        assertEquals(-90.0F, point.getYaw(), 1.0E-5F);
        assertEquals(0.0F, point.getPitch(), 0.0F);
    }

    @Test void nextStageDefinesTheSameNextFirstAxisAsTriggerGeometry() {
        LocationDefinition source = MobZoneGeometry.normal(1, 2, 0.0D, 0.0D, 30.0D);
        LocationDefinition previous = MobZoneGeometry.normal(1, 1, -40.0D, 0.0D, 30.0D);
        LocationDefinition next = MobZoneGeometry.normal(1, 3, 0.0D, 50.0D, 30.0D);

        AutoTransitionPointPlanner.Point point = planner.plan(source, previous, next, 5.0D, 70.0D);

        assertNotNull(point);
        assertEquals(0.0D, point.getX(), 1.0E-9D);
        assertEquals(-17.5D, point.getZ(), 1.0E-9D);
        assertEquals(0.0F, point.getYaw(), 1.0E-5F);
    }

    @Test void extremeTriggerOffsetIsClampedInsideMobZone() {
        LocationDefinition source = MobZoneGeometry.normal(1, 2, 10.0D, 20.0D, 10.0D);
        LocationDefinition previous = MobZoneGeometry.normal(1, 1, 0.0D, 20.0D, 10.0D);

        AutoTransitionPointPlanner.Point point = planner.plan(source, previous, null, 100.0D, 64.0D);

        assertNotNull(point);
        double dx = point.getX() - source.getCenterX();
        double dz = point.getZ() - source.getCenterZ();
        assertEquals(9.0D, Math.sqrt(dx * dx + dz * dz), 1.0E-9D);
        assertTrue(source.contains(point.getX(), point.getZ()));
    }

    @Test void noAdjacentStageDoesNotInventAnAxis() {
        LocationDefinition source = MobZoneGeometry.normal(1, 1, 0.0D, 0.0D, 30.0D);
        assertNull(planner.plan(source, null, null, 5.0D, 64.0D));
    }

    @Test void coincidentAdjacentCentersDoNotProduceNaNPoint() {
        LocationDefinition source = MobZoneGeometry.normal(1, 2, 0.0D, 0.0D, 30.0D);
        LocationDefinition previous = MobZoneGeometry.normal(1, 1, 0.0D, 0.0D, 30.0D);
        assertNull(planner.plan(source, previous, null, 5.0D, 64.0D));
    }
}
