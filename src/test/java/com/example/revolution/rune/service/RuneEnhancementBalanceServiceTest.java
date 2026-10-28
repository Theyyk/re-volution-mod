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
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneEnhancementBalanceServiceTest {
    @Test
    void bundledDefaultsPreserveCurrentPoisonBaseline() {
        RuneEnhancementBalanceService service = new RuneEnhancementBalanceService(BalanceBootstrap.activateBundledDefaults());

        assertEquals(ResourceDefinitions.RUNE_DUST, service.resource(RuneStatusConfig.STATUS_POISON));
        assertEquals(RuneEnhancementTargeting.ALL_SLOTS, service.targeting(RuneStatusConfig.STATUS_POISON));
        assertEquals(10, service.nextCost(RuneStatusConfig.STATUS_POISON, 0));
        assertEquals(10, service.nextCost(RuneStatusConfig.STATUS_POISON, 500));
        assertEquals(50, service.bulkCost(RuneStatusConfig.STATUS_POISON, 17, 5));
        assertEquals(5, service.totalEnhancementLevel(new int[] {2, 3}));
    }

    @Test
    void priceUsesCollectionTotalAndIsIndependentOfRandomTarget() {
        RuneEnhancementBalanceService balance = service("ALL_SLOTS",
                "{\"mode\":\"TABLE\",\"values\":[\"10\",\"20\",\"30\",\"40\",\"50\",\"60\"]}");
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(balance);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];

        RuneEnhancementPlan first = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[] {3, 0},
                owned,
                2,
                new Random(1L));
        RuneEnhancementPlan second = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[] {0, 3},
                owned,
                2,
                new Random(999L));

        assertEquals(3, first.getTotalEnhancementLevel());
        assertEquals(90, first.getTotalCost(), "global steps 3 and 4 cost 40 + 50");
        assertEquals(first.getTotalCost(), second.getTotalCost(),
                "random target and per-slot distribution must not affect price");
        assertEquals(2, sum(first.getIncrements()));
        assertEquals(2, sum(second.getIncrements()));
    }

    @Test
    void ownedOnlyNeverTargetsClosedSlots() {
        RuneEnhancementBalanceService balance = service("OWNED_ONLY", "{\"mode\":\"FIXED\",\"value\":\"10\"}");
        RuneEnhancementPlanningService planner = new RuneEnhancementPlanningService(balance);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        owned[4] = true;
        owned[7] = true;

        RuneEnhancementPlan plan = planner.plan(
                RuneStatusConfig.STATUS_POISON,
                new int[RuneInventory.BOOST_TYPE_COUNT],
                owned,
                100,
                new Random(42L));

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
    void rejectsFractionalOrUnsupportedEnhancementConfiguration() {
        RuneEnhancementBalanceService fractional = service(
                "ALL_SLOTS",
                "{\"mode\":\"FIXED\",\"value\":\"10.5\"}");
        assertThrows(IllegalStateException.class,
                () -> fractional.nextCost(RuneStatusConfig.STATUS_POISON, 0));

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

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }
}
