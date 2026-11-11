package com.example.revolution.rune.rules;

import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionProgressionTest {
    @Test
    void emptyCollectionStartsAtOrdinaryWithZeroProgress() {
        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(
                new int[RuneInventory.BOOST_TYPE_COUNT]);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(0, progression.higherRuneCount());
        assertEquals(0, progression.coveredCount());
    }

    @Test
    void oneImprovedRuneRestartsOrdinaryCollectionFromZero() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        ranks[0] = RuneProgression.IMPROVED_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(1, progression.higherRuneCount());
        assertEquals(1, progression.coveredCount());
        assertTrue(progression.isOwnedAtStage(0));
        assertFalse(progression.isOwnedAtStage(1));
    }

    @Test
    void improvedRuneAndRebuiltOrdinariesCompleteCurrentNestedCycle() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.ORDINARY_RANK);
        ranks[0] = RuneProgression.IMPROVED_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(25, progression.lowerProgress());
        assertEquals(1, progression.higherRuneCount());
        assertEquals(26, progression.coveredCount());
        assertTrue(progression.isComplete());
    }

    @Test
    void fullImprovedCollectionStillOnlyPurchasesOrdinary() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.IMPROVED_RANK);

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(26, progression.higherRuneCount());
        assertEquals(26, progression.coveredCount());
    }

    @Test
    void oneAdvancedRuneCoversOneOrdinarySlot() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        ranks[5] = RuneProgression.ADVANCED_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(1, progression.higherRuneCount());
        assertEquals(1, progression.coveredCount());
    }

    @Test
    void oneMythicRuneCoversOneOrdinarySlot() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        ranks[5] = RuneProgression.MYTHIC_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(1, progression.higherRuneCount());
        assertEquals(1, progression.coveredCount());
    }

    @Test
    void fullMythicCollectionBlocksOrdinaryPurchases() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.MYTHIC_RANK);

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(26, progression.higherRuneCount());
        assertTrue(progression.isComplete());
    }

    @Test
    void revertedAdvancedRuneProducesCompletedImprovedCycleWithoutHistoryCounter() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.ADVANCED_RANK);
        ranks[7] = RuneProgression.IMPROVED_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ORDINARY_RANK, progression.stageRank());
        assertEquals(0, progression.lowerProgress());
        assertEquals(26, progression.higherRuneCount());
        assertEquals(26, progression.coveredCount());
        assertTrue(progression.isComplete());
        assertTrue(progression.isOwnedAtStage(7));
    }
}
