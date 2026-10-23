package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;

public final class StageTransitionServiceTest {
    public static void main(String[] args) {
        entersTargetNearClosestEdge();
        fallsBackToCentersWhenPlayerMatchesTargetCenter();
        sameCenterStagesStaySafe();
        System.out.println("v1.3.9 stage transition geometry regression checks passed");
    }

    private static void entersTargetNearClosestEdge() {
        LocationDefinition source = new LocationDefinition(1000001, "Source", 0.0D, 0.0D, 10.0D);
        LocationDefinition target = new LocationDefinition(1000002, "Target", 30.0D, 0.0D, 10.0D);

        StageTransitionService.HorizontalPoint point = StageTransitionService.entryPoint(source, target, 8.0D, 0.0D);
        check(close(point.x, 21.5D), "Target entry must be 1.5 blocks inside the near edge for a radius-10 zone");
        check(close(point.z, 0.0D), "Straight-line transition must preserve the target-center Z axis");
        check(target.contains(point.x, point.z), "Resolved entry point must be inside the target zone");
        check(distance(target, point) < target.getRadius(), "Resolved entry point must be strictly inside the target boundary");
    }

    private static void fallsBackToCentersWhenPlayerMatchesTargetCenter() {
        LocationDefinition source = new LocationDefinition(1000001, "Source", 0.0D, 0.0D, 10.0D);
        LocationDefinition target = new LocationDefinition(1000002, "Target", 30.0D, 0.0D, 4.0D);

        StageTransitionService.HorizontalPoint point = StageTransitionService.entryPoint(source, target, 30.0D, 0.0D);
        check(close(point.x, 27.0D), "Center fallback must enter from the source-facing edge with radius-relative inset");
        check(close(point.z, 0.0D), "Center fallback must retain the source-target axis");
        check(target.contains(point.x, point.z), "Fallback entry must remain inside the target zone");
    }

    private static void sameCenterStagesStaySafe() {
        LocationDefinition source = new LocationDefinition(1000001, "Source", 5.0D, -2.0D, 8.0D);
        LocationDefinition target = new LocationDefinition(1000002, "Target", 5.0D, -2.0D, 6.0D);

        StageTransitionService.HorizontalPoint point = StageTransitionService.entryPoint(source, target, 5.0D, -2.0D);
        check(close(point.x, 5.0D) && close(point.z, -2.0D),
                "Coincident stage centers must safely resolve to the shared center instead of producing NaN");
        check(target.contains(point.x, point.z), "Shared-center fallback must be inside target geometry");
    }

    private static double distance(LocationDefinition location, StageTransitionService.HorizontalPoint point) {
        double dx = point.x - location.getCenterX();
        double dz = point.z - location.getCenterZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-9D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
