package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RunePurchaseBalanceServiceTest {
    @Test
    void appliesHigherRankPressureAndRoundsHalfUp() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.purchase.higher_rank_multiplier\":\"1.15\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\","
                + "\"runes.poison.duplicate.rate\":\"0.0003\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"20\",\"43\",\"5290\"]}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        assertEquals("coins", service.purchaseResource(RuneStatusConfig.STATUS_POISON));
        assertEquals(ResourceDefinitions.RUNE_DUST, service.duplicateResource(RuneStatusConfig.STATUS_POISON));
        assertEquals(20, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0, 0));
        assertEquals(23, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0, 1));
        assertEquals(26, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0, 2));
        assertEquals(49, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1, 1));
        assertEquals(1, service.duplicateReward(RuneStatusConfig.STATUS_POISON, 1, 0, 1));
        assertEquals(2, service.duplicateReward(RuneStatusConfig.STATUS_POISON, 1, 2, 0));
    }

    @Test
    void duplicateDoesNotAdvanceFactualProgressionPoint() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.purchase.higher_rank_multiplier\":\"1.15\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\","
                + "\"runes.poison.duplicate.rate\":\"0.0003\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"100\",\"125\",\"150\"]}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        int before = service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1, 2);
        int duplicate = service.duplicateReward(RuneStatusConfig.STATUS_POISON, 1, 1, 2);
        int after = service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1, 2);
        assertEquals(before, after);
        assertEquals(1, duplicate);
    }

    @Test
    void legacyBalanceWithoutPressureOrRateKeepsOldBehavior() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"100\",\"125\"]},"
                + "\"runes.poison.duplicate.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"1\",\"2\"]}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        assertEquals(125, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1, 7));
        assertEquals(2, service.duplicateReward(RuneStatusConfig.STATUS_POISON, 1, 1, 7));
    }

    @Test
    void bundledDefaultsCoverEveryCurrentlySupportedPurchaseRank() {
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(BalanceBootstrap.activateBundledDefaults());

        for (int rank = 1; rank <= 3; rank++) {
            assertEquals(100, service.purchaseCost(RuneStatusConfig.STATUS_POISON, rank, 0, 0));
            assertEquals(100, service.purchaseCost(RuneStatusConfig.STATUS_POISON, rank, 25, 0));
            assertEquals(1, service.duplicateReward(RuneStatusConfig.STATUS_POISON, rank, 0, 0));
            assertEquals(1, service.duplicateReward(RuneStatusConfig.STATUS_POISON, rank, 25, 0));
        }
    }

    @Test
    void rejectsUnconfiguredRanksAndInvalidPressure() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.purchase.higher_rank_multiplier\":\"0\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\","
                + "\"runes.poison.duplicate.rate\":\"0.0003\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"FIXED\",\"value\":\"100\"}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        assertThrows(IllegalStateException.class,
                () -> service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.purchaseCost(RuneStatusConfig.STATUS_POISON, 2, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0, -1));
    }

    private static BalanceService newService() {
        return new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                new BalanceConfigCodec(),
                new BalanceCurveEngine());
    }
}
