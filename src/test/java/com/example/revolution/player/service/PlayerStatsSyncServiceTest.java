package com.example.revolution.player.service;

import com.example.revolution.rune.rules.RuneCollectionProgression;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class PlayerStatsSyncServiceTest {
    @Test
    void completeImprovedStageReturnsZeroWithoutPurchaseCurveLookup() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.IMPROVED_RANK);
        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(26, progression.lowerProgress());
        assertEquals(0, progression.higherRuneCount());
        assertEquals(true, progression.isComplete());
        assertEquals(0, PlayerStatsSyncService.resolvePurchaseUnitCost(null, 1, progression));
    }

    @Test
    void completedNestedCycleReturnsZeroWithoutPurchaseCurveLookup() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.NORMAL_RANK);
        ranks[0] = RuneProgression.IMPROVED_RANK;
        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(ranks);

        assertEquals(25, progression.lowerProgress());
        assertEquals(1, progression.higherRuneCount());
        assertEquals(true, progression.isComplete());
        assertEquals(0, PlayerStatsSyncService.resolvePurchaseUnitCost(null, 1, progression));
    }
}
