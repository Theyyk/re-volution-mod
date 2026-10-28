package com.example.revolution.rune.rules;

import com.example.revolution.rune.state.RuneInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionProgressionTest {
    @Test
    void emptyCollectionStartsAtNormalWithZeroProgress() {
        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(
                new int[RuneInventory.BOOST_TYPE_COUNT]);

        assertEquals(RuneProgression.NORMAL_RANK, progression.stageRank());
        assertEquals(0, progression.progressionIndex());
    }

    @Test
    void derivesStageAndIndexOnlyFromFactualCurrentRanks() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        ranks[0] = RuneProgression.ADVANCED_RANK;
        ranks[1] = RuneProgression.ADVANCED_RANK;
        ranks[2] = RuneProgression.ADVANCED_RANK;
        ranks[3] = RuneProgression.IMPROVED_RANK;

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ADVANCED_RANK, progression.stageRank());
        assertEquals(3, progression.progressionIndex());
        assertTrue(progression.isOwnedAtStage(0));
        assertFalse(progression.isOwnedAtStage(3),
                "a lower-rank rune must not count as acquired on the active stage");
    }

    @Test
    void loweringOneRuneAutomaticallyLowersStagePopulationWithoutHistoryMutation() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        ranks[0] = RuneProgression.ADVANCED_RANK;
        ranks[1] = RuneProgression.ADVANCED_RANK;
        ranks[2] = RuneProgression.ADVANCED_RANK;
        assertEquals(3, RuneCollectionProgression.fromBestRanks(ranks).progressionIndex());

        ranks[2] = RuneProgression.IMPROVED_RANK;
        RuneCollectionProgression afterLocalRankChange = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(RuneProgression.ADVANCED_RANK, afterLocalRankChange.stageRank());
        assertEquals(2, afterLocalRankChange.progressionIndex());
    }
}
