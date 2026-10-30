package com.example.revolution.rune.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RunePrestigeResetRulesTest {
    @Test
    void ordinaryAndImprovedResetToOrdinary() {
        assertTrue(RunePrestigeResetRules.shouldReset(RuneProgression.ORDINARY_RANK));
        assertTrue(RunePrestigeResetRules.shouldReset(RuneProgression.IMPROVED_RANK));
        assertEquals(RuneProgression.ORDINARY_RANK,
                RunePrestigeResetRules.rankAfterReset(RuneProgression.ORDINARY_RANK));
        assertEquals(RuneProgression.ORDINARY_RANK,
                RunePrestigeResetRules.rankAfterReset(RuneProgression.IMPROVED_RANK));
    }

    @Test
    void advancedMythicAndFutureHigherRanksArePreserved() {
        assertFalse(RunePrestigeResetRules.shouldReset(RuneProgression.ADVANCED_RANK));
        assertFalse(RunePrestigeResetRules.shouldReset(RuneProgression.MYTHIC_RANK));
        assertFalse(RunePrestigeResetRules.shouldReset(RuneProgression.MYTHIC_RANK + 100));
        assertEquals(RuneProgression.ADVANCED_RANK,
                RunePrestigeResetRules.rankAfterReset(RuneProgression.ADVANCED_RANK));
        assertEquals(RuneProgression.MYTHIC_RANK,
                RunePrestigeResetRules.rankAfterReset(RuneProgression.MYTHIC_RANK));
        assertEquals(RuneProgression.MYTHIC_RANK + 100,
                RunePrestigeResetRules.rankAfterReset(RuneProgression.MYTHIC_RANK + 100));
    }

    @Test
    void missingRuneIsNotCreatedByPrestigeReset() {
        assertFalse(RunePrestigeResetRules.shouldReset(0));
        assertEquals(0, RunePrestigeResetRules.rankAfterReset(0));
    }
}
