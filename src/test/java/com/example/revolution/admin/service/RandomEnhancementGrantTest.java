package com.example.revolution.admin.service;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomEnhancementGrantTest {
    @Test void plansExactRequestedCountAcrossExistingSlots() {
        int[] levels = new int[26];
        AtomicInteger next = new AtomicInteger();
        int[] increments = RandomEnhancementGrant.plan(levels, 1000, bound -> next.getAndIncrement() % bound);
        assertEquals(1000, Arrays.stream(increments).sum());
        assertTrue(Arrays.stream(increments).allMatch(value -> value >= 38 && value <= 39));
        assertArrayEquals(new int[26], levels, "planning must not mutate persisted levels");
    }

    @Test void rejectsInvalidCountAndRandomSlotBeforeReturningPlan() {
        assertThrows(IllegalArgumentException.class, () -> RandomEnhancementGrant.plan(new int[26], 0, bound -> 0));
        assertThrows(IllegalArgumentException.class, () -> RandomEnhancementGrant.plan(new int[26], 100001, bound -> 0));
        assertThrows(IllegalArgumentException.class, () -> RandomEnhancementGrant.plan(new int[26], 1, bound -> bound));
    }

    @Test void rejectsOverflowWithoutMutatingInput() {
        int[] levels = new int[26]; levels[0] = Integer.MAX_VALUE;
        assertThrows(IllegalArgumentException.class, () -> RandomEnhancementGrant.plan(levels, 1, bound -> 0));
        assertEquals(Integer.MAX_VALUE, levels[0]);
    }
}
