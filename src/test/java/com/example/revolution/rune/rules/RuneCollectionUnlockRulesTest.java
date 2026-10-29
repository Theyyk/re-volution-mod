package com.example.revolution.rune.rules;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionUnlockRulesTest {
    @Test
    void exactlyThirteenImprovedOrHigherQualifies() {
        int[] ranks = new int[26];
        Arrays.fill(ranks, RuneProgression.ORDINARY_RANK);
        for (int i = 0; i < 13; i++) ranks[i] = RuneProgression.IMPROVED_RANK;

        assertEquals(13, RuneCollectionUnlockRules.countImprovedOrHigher(ranks));
        assertTrue(RuneCollectionUnlockRules.qualifies(ranks, 13));
    }

    @Test
    void advancedAndMythicAlsoCountTowardUnlock() {
        int[] ranks = new int[26];
        for (int i = 0; i < 5; i++) ranks[i] = RuneProgression.IMPROVED_RANK;
        for (int i = 5; i < 10; i++) ranks[i] = RuneProgression.ADVANCED_RANK;
        for (int i = 10; i < 13; i++) ranks[i] = RuneProgression.MYTHIC_RANK;

        assertEquals(13, RuneCollectionUnlockRules.countImprovedOrHigher(ranks));
        assertTrue(RuneCollectionUnlockRules.qualifies(ranks, 13));
    }

    @Test
    void historicalProgressIsIrrelevantWhenCurrentRanksDoNotQualify() {
        int[] ranks = new int[26];
        Arrays.fill(ranks, RuneProgression.ORDINARY_RANK);
        ranks[0] = RuneProgression.IMPROVED_RANK;

        assertEquals(1, RuneCollectionUnlockRules.countImprovedOrHigher(ranks));
        assertFalse(RuneCollectionUnlockRules.qualifies(ranks, 13));
    }

    @Test
    void invalidThresholdNeverQualifies() {
        int[] ranks = new int[26];
        Arrays.fill(ranks, RuneProgression.MYTHIC_RANK);
        assertFalse(RuneCollectionUnlockRules.qualifies(ranks, 0));
        assertFalse(RuneCollectionUnlockRules.qualifies(ranks, 27));
    }
}
