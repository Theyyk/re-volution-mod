package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneEnhancementTargeting;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneEnhancementBalanceServiceTest {
    @Test
    void bundledDefaultsKeepExplicitCurveAndContinueWithUnlimitedTail() {
        RuneEnhancementBalanceService service = new RuneEnhancementBalanceService(BalanceBootstrap.activateBundledDefaults());

        assertEquals(ResourceDefinitions.RUNE_DUST, service.resource(RuneStatusConfig.STATUS_POISON));
        assertEquals(RuneEnhancementTargeting.ALL_SLOTS, service.targeting(RuneStatusConfig.STATUS_POISON));
        assertCost("1", service.nextCost(RuneStatusConfig.STATUS_POISON, 0));
        assertCost("16", service.nextCost(RuneStatusConfig.STATUS_POISON, 9));
        assertCost("12440", service.nextCost(RuneStatusConfig.STATUS_POISON, 49));
        assertCost("13684", service.nextCost(RuneStatusConfig.STATUS_POISON, 50));
        assertCost("32266", service.nextCost(RuneStatusConfig.STATUS_POISON, 59));
        assertCost("134784", service.nextCost(RuneStatusConfig.STATUS_POISON, 74));
        assertCost("1460342", service.nextCost(RuneStatusConfig.STATUS_POISON, 99));
        assertCost("171430818", service.nextCost(RuneStatusConfig.STATUS_POISON, 149));
        assertCost("20124409877", service.nextCost(RuneStatusConfig.STATUS_POISON, 199));

        assertCost("73", service.bulkCost(RuneStatusConfig.STATUS_POISON, 0, 10));
        assertCost("531", service.bulkCost(RuneStatusConfig.STATUS_POISON, 0, 20));
        assertCost("2936", service.bulkCost(RuneStatusConfig.STATUS_POISON, 0, 30));
        assertCost("15550", service.bulkCost(RuneStatusConfig.STATUS_POISON, 0, 40));
        assertCost("81520", service.bulkCost(RuneStatusConfig.STATUS_POISON, 0, 50));
        assertTrue(service.bulkCost(RuneStatusConfig.STATUS_POISON, 150, 100).signum() > 0);
        assertEquals(5, service.totalEnhancementLevel(new int[] {2, 3}));
    }

    @Test
    void priceUsesFactualCollectionTotalAndIsIndependentOfRandomTarget() {
        RuneEnhancementBalanceService balance = service("ALL_SLOTS",
                "{\"mode\":\"TABLE\",\"values\":[\"10\",\"20\",\"30\",\"40\",\"50\",\"60\"],\"tail_growth\":\"2\"}");
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(balance);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];

        RuneEnhancementPlan first = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[] {3, 0}, owned, 2, new Random(1L));
        RuneEnhancementPlan second = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[] {0, 3}, owned, 2, new Random(999L));

        assertEquals(3, first.getTotalEnhancementLevel());
        assertCost("90", first.getTotalCost());
        assertEquals(first.getTotalCost(), second.getTotalCost(),
                "random target and per-slot distribution must not affect price");
        assertEquals(2, sum(first.getIncrements()));
        assertEquals(2, sum(second.getIncrements()));
    }

    @Test
    void allSlotsTargetingStillIncludesEmptySlotsPastLegacyFiftyBoundary() {
        RuneEnhancementBalanceService balance = service("ALL_SLOTS",
                "{\"mode\":\"TABLE\",\"values\":[\"10\"],\"tail_growth\":\"1\"}");
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(balance);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        owned[4] = true;

        RuneEnhancementPlan plan = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                levelsWithTotal(50),
                owned,
                100,
                new Random(42L));

        assertEquals(50, plan.getTotalEnhancementLevel());
        assertCost("1000", plan.getTotalCost());
        assertEquals(100, sum(plan.getIncrements()));
        int emptySlotHits = 0;
        for (int slot = 0; slot < plan.getIncrements().length; slot++) {
            if (!owned[slot]) emptySlotHits += plan.getIncrements()[slot];
        }
        assertTrue(emptySlotHits > 0, "ALL_SLOTS must continue to include empty slots");
    }

    @Test
    void ownedOnlyTargetsOnlyOwnedSlotsWithoutLegacyCap() {
        RuneEnhancementBalanceService balance = service("OWNED_ONLY",
                "{\"mode\":\"FIXED\",\"value\":\"10\"}");
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(balance);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        owned[4] = true;
        owned[7] = true;

        RuneEnhancementPlan plan = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                levelsWithTotal(75), owned, 100, new Random(42L));

        int[] increments = plan.getIncrements();
        assertEquals(RuneEnhancementTargeting.OWNED_ONLY, plan.getTargeting());
        assertEquals(100, sum(increments));
        assertTrue(increments[4] > 0);
        assertTrue(increments[7] > 0);
        for (int slot = 0; slot < increments.length; slot++) {
            if (slot == 4 || slot == 7) continue;
            assertEquals(0, increments[slot]);
        }
    }

    @Test
    void ownedOnlyRejectsCollectionWithoutEligibleRunes() {
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(
                service("OWNED_ONLY", "{\"mode\":\"FIXED\",\"value\":\"10\"}"));

        assertThrows(IllegalStateException.class, () -> planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[RuneInventory.BOOST_TYPE_COUNT],
                new boolean[RuneInventory.BOOST_TYPE_COUNT],
                1,
                new Random(0L)));
    }

    @Test
    void roundsFormulaPricesAndRejectsUnsupportedTargeting() {
        RuneEnhancementBalanceService fractional = service(
                "ALL_SLOTS",
                "{\"mode\":\"FIXED\",\"value\":\"10.5\"}");
        assertCost("11", fractional.nextCost(RuneStatusConfig.STATUS_POISON, 0));

        RuneEnhancementBalanceService unsupported = service(
                "SOMETHING_ELSE",
                "{\"mode\":\"FIXED\",\"value\":\"10\"}");
        assertThrows(IllegalStateException.class,
                () -> unsupported.targeting(RuneStatusConfig.STATUS_POISON));
    }

    private static RuneEnhancementBalanceService service(String targeting, String curveJson) {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                new BalanceConfigCodec(),
                new BalanceCurveEngine());
        balance.activate("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.enhancement.resource\":\"rune_dust\","
                + "\"runes.poison.enhancement.targeting\":\"" + targeting + "\"},\"curves\":{"
                + "\"runes.poison.enhancement.cost\":" + curveJson + "}}");
        return new RuneEnhancementBalanceService(balance);
    }

    private static int[] levelsWithTotal(int total) {
        int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        levels[0] = total;
        return levels;
    }

    private static void assertCost(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected cost " + expected + " but was " + actual);
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }
}
