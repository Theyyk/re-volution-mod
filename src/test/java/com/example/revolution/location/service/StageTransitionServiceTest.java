package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import java.util.Arrays;

public final class StageTransitionServiceTest {
    public static void main(String[] args) {
        legacyLocationHasNoTransitionPoint();
        explicitTransitionPointRetainsCoordinatesAndRotation();
        transitionPointMayBeOutsideMobZoneGeometry();
        transitionPointMayBeAnywhereInWorldCoordinates();
        clearedTransitionPointHasNoDestination();
        nearestConfiguredStageUsesTransitionPointAcrossLocations();
        transitionPointMustBeFinite();
        System.out.println("v1.3.9 explicit stage transition point regression checks passed");
    }

    private static void legacyLocationHasNoTransitionPoint() {
        LocationDefinition location = new LocationDefinition(1000001, "Legacy", 0.0D, 0.0D, 10.0D);
        check(!location.hasTransitionPoint(), "Legacy location constructor must remain compatible and leave transition point unset");
    }

    private static void explicitTransitionPointRetainsCoordinatesAndRotation() {
        LocationDefinition location = new LocationDefinition(
                MobZoneIds.encode(1, 1),
                "Configured",
                30.0D,
                -4.0D,
                12.0D,
                28.5D,
                71.0D,
                -3.5D,
                90.0F,
                12.5F);

        check(location.hasTransitionPoint(), "Explicit constructor must mark transition point configured");
        check(close(location.getTransitionX(), 28.5D), "Transition X must be retained");
        check(close(location.getTransitionY(), 71.0D), "Transition Y must be retained");
        check(close(location.getTransitionZ(), -3.5D), "Transition Z must be retained");
        check(Math.abs(location.getTransitionYaw() - 90.0F) < 1.0E-6F, "Transition yaw must be retained");
        check(Math.abs(location.getTransitionPitch() - 12.5F) < 1.0E-6F, "Transition pitch must be retained");
    }

    private static void transitionPointMayBeOutsideMobZoneGeometry() {
        LocationDefinition stage = new LocationDefinition(
                MobZoneIds.encode(1, 2),
                "Stage 2",
                100.0D,
                100.0D,
                5.0D,
                80.0D,
                64.0D,
                80.0D,
                0.0F,
                0.0F);
        check(!stage.contains(stage.getTransitionX(), stage.getTransitionZ()),
                "Transition point must be representable outside mob-zone geometry");
    }

    private static void transitionPointMayBeAnywhereInWorldCoordinates() {
        LocationDefinition stage = new LocationDefinition(
                MobZoneIds.encode(1, 3),
                "Stage 3",
                0.0D,
                0.0D,
                5.0D,
                50000.0D,
                200.0D,
                -50000.0D,
                180.0F,
                0.0F);
        check(stage.hasTransitionPoint(), "Far-away transition point must remain valid");
        check(!stage.contains(stage.getTransitionX(), stage.getTransitionZ()),
                "Far-away transition point must not be constrained by stage geometry");
    }

    private static void clearedTransitionPointHasNoDestination() {
        LocationDefinition configured = new LocationDefinition(
                MobZoneIds.encode(1, 4), "Stage 4", 10.0D, 10.0D, 3.0D,
                100.0D, 70.0D, 100.0D, 45.0F, 5.0F);
        LocationDefinition cleared = new LocationDefinition(
                configured.getLocationId(), configured.getName(), configured.getCenterX(), configured.getCenterZ(), configured.getRadius());
        check(!cleared.hasTransitionPoint(), "Cleared stage definition must have no transition destination");
    }

    private static void nearestConfiguredStageUsesTransitionPointAcrossLocations() {
        LocationDefinition first = new LocationDefinition(
                MobZoneIds.encode(1, 1), "Stage 1", 10.0D, 10.0D, 3.0D,
                20.0D, 64.0D, 20.0D, 0.0F, 0.0F);
        LocationDefinition second = new LocationDefinition(
                MobZoneIds.encode(1, 2), "Stage 2", 40.0D, 40.0D, 3.0D,
                60.0D, 64.0D, 60.0D, 0.0F, 0.0F);
        LocationDefinition otherLocation = new LocationDefinition(
                MobZoneIds.encode(2, 1), "Other", 22.0D, 22.0D, 3.0D,
                21.0D, 64.0D, 21.0D, 0.0F, 0.0F);

        LocationDefinition resolved = StageTransitionService.nearestConfiguredStage(
                Arrays.asList(first, second, otherLocation), 23.0D, 23.0D);
        check(resolved != null && resolved.getLocationId() == otherLocation.getLocationId(),
                "Outside zones, nearest configured transition point may resolve across world Locations");
    }

    private static void transitionPointMustBeFinite() {
        boolean failed = false;
        try {
            new LocationDefinition(
                    MobZoneIds.encode(1, 3),
                    "Invalid",
                    0.0D,
                    0.0D,
                    5.0D,
                    Double.NaN,
                    64.0D,
                    0.0D,
                    0.0F,
                    0.0F);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        check(failed, "Non-finite transition coordinates must be rejected");
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-9D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
