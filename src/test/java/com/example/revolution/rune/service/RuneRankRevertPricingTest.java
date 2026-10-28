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

/** Verifies that revert changes the next data-driven purchase price through factual ranks only. */
public final class RuneRankRevertPricingTest {
    @Test
    void nextPurchasePriceIsRecomputedFromRevertedCollection() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.3\":{\"mode\":\"TABLE\",\"values\":["
                + "\"100\",\"101\",\"102\",\"103\",\"104\",\"105\",\"106\",\"107\",\"108\","
                + "\"109\",\"110\",\"111\",\"112\",\"113\",\"114\",\"115\",\"116\",\"117\","
                + "\"118\",\"119\",\"120\",\"121\",\"122\",\"123\",\"124\",\"125\",\"126\"]},"
                + "\"runes.poison.duplicate.rank.3\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");
        RunePurchaseBalanceService purchase = new RunePurchaseBalanceService(balance);

        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, RuneProgression.ADVANCED_RANK);
        RuneRankRevertDecision first = RuneRankRevertDecision.evaluate(ranks, 4);
        ranks[4] = first.targetRank;

        RuneCollectionProgression after = RuneCollectionProgression.fromBestRanks(ranks);
        assertEquals(RuneProgression.ADVANCED_RANK, after.stageRank());
        assertEquals(25, after.progressionIndex());
        assertEquals(125, purchase.purchaseCost(
                RuneStatusConfig.STATUS_POISON, after.stageRank(), after.progressionIndex()));

        ranks[4] = RuneProgression.ADVANCED_RANK;
        RuneRankRevertDecision second = RuneRankRevertDecision.evaluate(ranks, 4);
        ranks[4] = second.targetRank;
        RuneCollectionProgression afterAgain = RuneCollectionProgression.fromBestRanks(ranks);
        assertEquals(after.progressionIndex(), afterAgain.progressionIndex());
        assertEquals(125, purchase.purchaseCost(
                RuneStatusConfig.STATUS_POISON, afterAgain.stageRank(), afterAgain.progressionIndex()));
    }
}
