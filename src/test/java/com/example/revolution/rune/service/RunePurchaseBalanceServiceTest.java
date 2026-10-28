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
    void pricesByOpenedSlotsAndKeepsDuplicateProgressionStable() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"100\",\"125\",\"150\"]},"
                + "\"runes.poison.duplicate.rank.1\":{\"mode\":\"TABLE\",\"values\":[\"1\",\"2\",\"3\"]}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        assertEquals("coins", service.purchaseResource(RuneStatusConfig.STATUS_POISON));
        assertEquals(ResourceDefinitions.RUNE_DUST, service.duplicateResource(RuneStatusConfig.STATUS_POISON));
        assertEquals(100, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0));
        assertEquals(125, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1));
        assertEquals(2, service.duplicateReward(RuneStatusConfig.STATUS_POISON, 1, 1));
        assertEquals(125, service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 1),
                "a duplicate does not advance openedSlots, so the next attempt keeps the same price point");
    }

    @Test
    void bundledDefaultsCoverEveryCurrentlySupportedPurchaseRank() {
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(BalanceBootstrap.activateBundledDefaults());

        for (int rank = 1; rank <= 3; rank++) {
            assertEquals(100, service.purchaseCost(RuneStatusConfig.STATUS_POISON, rank, 0));
            assertEquals(100, service.purchaseCost(RuneStatusConfig.STATUS_POISON, rank, 25));
            assertEquals(1, service.duplicateReward(RuneStatusConfig.STATUS_POISON, rank, 0));
            assertEquals(1, service.duplicateReward(RuneStatusConfig.STATUS_POISON, rank, 25));
        }
    }

    @Test
    void rejectsUnconfiguredRanksAndFractionalResourceAmounts() {
        BalanceService balance = newService();
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.duplicate.resource\":\"rune_dust\"},\"curves\":{"
                + "\"runes.poison.purchase.rank.1\":{\"mode\":\"FIXED\",\"value\":\"100.5\"},"
                + "\"runes.poison.duplicate.rank.1\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");
        RunePurchaseBalanceService service = new RunePurchaseBalanceService(balance);

        assertThrows(IllegalStateException.class,
                () -> service.purchaseCost(RuneStatusConfig.STATUS_POISON, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.purchaseCost(RuneStatusConfig.STATUS_POISON, 2, 0));
    }

    private static BalanceService newService() {
        return new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                new BalanceConfigCodec(),
                new BalanceCurveEngine());
    }
}
