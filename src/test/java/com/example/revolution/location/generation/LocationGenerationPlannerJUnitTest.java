package com.example.revolution.location.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public final class LocationGenerationPlannerJUnitTest {
    private final LocationGenerationPlanner planner = new LocationGenerationPlanner();
    private final LocationTemplate template = LocationTemplate.defaultStage();

    @Test
    void physicalRotationIsSeedOnlyAndAlwaysOneOfFourQuarterTurns() {
        java.util.Set<Integer> rotations = new java.util.HashSet<>();
        long[] extremes = {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L};
        for (long seed : extremes) assertQuarterTurn(seed, rotations);
        for (long seed = -2048; seed <= 2048; seed++) assertQuarterTurn(seed, rotations);
        assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(0, 90, 180, 270)), rotations);
    }

    private void assertQuarterTurn(long seed, java.util.Set<Integer> rotations) {
        int expected = planner.rotationForSeed(seed);
        assertTrue(expected == 0 || expected == 90 || expected == 180 || expected == 270);
        rotations.add(expected);
        LocationGenerationPlan first = planner.plan(template, 1, 1, 10, 64, -10, seed);
        LocationGenerationPlan other = planner.plan(new LocationTemplate("other", 15, java.util.Collections.emptyList()),
                7, 99, -20, 70, 100, seed);
        assertEquals(expected, first.getRotationDegrees());
        assertEquals(expected, other.getRotationDegrees());
        assertEquals(expected, planner.rotationForSeed(seed));
    }

    @Test
    void sameSeedAndAnchorProduceIdenticalPlan() {
        LocationGenerationPlan first = planner.plan(template, 1, 4, 100.5D, 70.0D, -20.5D, 123456789L);
        LocationGenerationPlan second = planner.plan(template, 1, 4, 100.5D, 70.0D, -20.5D, 123456789L);

        assertEquals(first.getInternalLocationId(), second.getInternalLocationId());
        assertEquals(first.getRotationDegrees(), second.getRotationDegrees());
        assertEquals(first.getDecorations().size(), second.getDecorations().size());
        for (int i = 0; i < first.getDecorations().size(); i++) {
            LocationGenerationPlan.DecorationPlacement a = first.getDecorations().get(i);
            LocationGenerationPlan.DecorationPlacement b = second.getDecorations().get(i);
            assertEquals(a.getKind(), b.getKind());
            assertEquals(a.getOffsetX(), b.getOffsetX(), 0.0D);
            assertEquals(a.getOffsetZ(), b.getOffsetZ(), 0.0D);
            assertEquals(a.getRotationDegrees(), b.getRotationDegrees());
        }
    }

    @Test
    void differentSeedsProduceDifferentVariation() {
        LocationGenerationPlan first = planner.plan(template, 1, 4, 0.0D, 64.0D, 0.0D, 100L);
        LocationGenerationPlan second = planner.plan(template, 1, 4, 0.0D, 64.0D, 0.0D, 101L);
        assertNotEquals(signature(first), signature(second));
    }

    @Test
    void plannedDecorationsStayInsideTemplateRadius() {
        LocationGenerationPlan plan = planner.plan(template, 7, 2, 20.0D, 80.0D, 30.0D, -5L);
        assertEquals(template.getDecorationKinds().size(), plan.getDecorations().size());
        for (LocationGenerationPlan.DecorationPlacement placement : plan.getDecorations()) {
            double distance = Math.sqrt(
                    placement.getOffsetX() * placement.getOffsetX()
                            + placement.getOffsetZ() * placement.getOffsetZ());
            assertTrue(distance >= template.getRadius() * 0.30D);
            assertTrue(distance <= template.getRadius() * 0.75D);
        }
    }

    @Test
    void defaultSeedIsStableForSameWorldAndAnchor() {
        long first = planner.defaultSeed(987654321L, 3, 5, 100, 64, -200);
        long second = planner.defaultSeed(987654321L, 3, 5, 100, 64, -200);
        long moved = planner.defaultSeed(987654321L, 3, 5, 101, 64, -200);
        assertEquals(first, second);
        assertNotEquals(first, moved);
    }

    private static String signature(LocationGenerationPlan plan) {
        StringBuilder value = new StringBuilder().append(plan.getRotationDegrees());
        for (LocationGenerationPlan.DecorationPlacement placement : plan.getDecorations()) {
            value.append('|').append(placement.getKind())
                    .append(':').append(placement.getOffsetX())
                    .append(':').append(placement.getOffsetZ())
                    .append(':').append(placement.getRotationDegrees());
        }
        return value.toString();
    }
}
