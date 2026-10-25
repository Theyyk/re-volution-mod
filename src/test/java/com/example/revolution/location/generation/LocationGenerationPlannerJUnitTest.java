package com.example.revolution.location.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public final class LocationGenerationPlannerJUnitTest {
    private final LocationGenerationPlanner planner = new LocationGenerationPlanner();
    private final LocationTemplate template = LocationTemplate.defaultStage();

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
