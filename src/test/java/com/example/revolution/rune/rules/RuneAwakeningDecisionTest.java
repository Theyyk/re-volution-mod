package com.example.revolution.rune.rules;

import com.example.revolution.rune.state.RuneInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneAwakeningDecisionTest {
    @Test
    void usesConfiguredTargetAndRequiredSlotCount() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (int i = 0; i < 10; i++) ranks[i] = RuneProgression.NORMAL_RANK;

        RuneAwakeningDecision allowed = RuneAwakeningDecision.evaluate(
                ranks,
                0,
                RuneProgression.IMPROVED_RANK,
                10);

        assertTrue(allowed.isAllowed());
        assertEquals(RuneProgression.IMPROVED_RANK, allowed.nextRank);

        ranks[9] = 0;
        RuneAwakeningDecision blocked = RuneAwakeningDecision.evaluate(
                ranks,
                0,
                RuneProgression.IMPROVED_RANK,
                10);
        assertEquals(RuneAwakeningDecision.Reason.COLLECTION_BEHIND, blocked.reason);
    }
}
