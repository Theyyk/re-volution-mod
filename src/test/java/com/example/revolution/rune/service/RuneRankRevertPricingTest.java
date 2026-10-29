package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.rules.RuneCollectionProgression;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.rules.RuneRankRevertDecision;
import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that revert derives the same nested price point from factual ranks on every cycle. */
public final class RuneRankRevertPricingTest {
    @Test
    void revertedFullAdvancedCollectionIsCoveredAndPriceStateIsStable() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.purchase.higher_rank_multiplier\":\"1.15\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\","
                + "\"runes.poison.duplicate.rate\":\"0.0003\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.2\":{\"mode\":\"TABLE\",\"values\":[\"20\",\"40\"]}}}");
        RunePurchaseBalanceService purchase = new RunePurchaseBalanceService(balance);

        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.ADVANCED_RANK);
        RuneRankRevertDecision first = RuneRankRevertDecision.evaluate(ranks, 4);
        ranks[4] = first.targetRank;

        RuneCollectionProgression after = RuneCollectionProgression.fromBestRanks(ranks);
        assertEquals(RuneProgression.IMPROVED_RANK, after.stageRank());
        assertEquals(1, after.lowerProgress());
        assertEquals(25, after.higherRuneCount());
        assertEquals(26, after.coveredCount());
        assertTrue(after.isComplete(), "revert must not create a purchase-farm slot");
        int factualPricePoint = purchase.purchaseCost(
                RuneStatusConfig.STATUS_POISON,
                after.stageRank(),
                after.lowerProgress(),
                after.higherRuneCount());

        ranks[4] = RuneProgression.ADVANCED_RANK;
        RuneRankRevertDecision second = RuneRankRevertDecision.evaluate(ranks, 4);
        ranks[4] = second.targetRank;
        RuneCollectionProgression afterAgain = RuneCollectionProgression.fromBestRanks(ranks);
        int repeatedPricePoint = purchase.purchaseCost(
                RuneStatusConfig.STATUS_POISON,
                afterAgain.stageRank(),
                afterAgain.lowerProgress(),
                afterAgain.higherRuneCount());

        assertEquals(after.stageRank(), afterAgain.stageRank());
        assertEquals(after.lowerProgress(), afterAgain.lowerProgress());
        assertEquals(after.higherRuneCount(), afterAgain.higherRuneCount());
        assertEquals(factualPricePoint, repeatedPricePoint,
                "revert/rank-up/revert must not accumulate hidden price state");
    }
}
