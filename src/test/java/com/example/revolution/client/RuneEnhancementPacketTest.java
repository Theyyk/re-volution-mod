package com.example.revolution.client;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneEconomyConfig;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.service.RuneEnhancementBalanceService;
import com.example.revolution.rune.service.RunePacketFactory;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;

public final class RuneEnhancementPacketTest {
    public static void main(String[] args) {
        int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        levels[0] = 0;
        levels[1] = 10;
        int statusId = RuneStatusConfig.STATUS_POISON;
        RuneEnhancementBalanceService enhancementBalance =
                new RuneEnhancementBalanceService(BalanceBootstrap.activateBundledDefaults());
        int totalEnhancementLevel = enhancementBalance.totalEnhancementLevel(levels);
        BigDecimal[] bulkCosts = enhancementBalance.bulkCosts(
                statusId,
                totalEnhancementLevel,
                RuneEnhancementPacket.BULK_AMOUNTS);

        RuneEnhancementPacket packet = RunePacketFactory.enhancements(
                statusId,
                enhancementBalance.resource(statusId),
                RuneEconomyConfig.enhancementPowerStep(statusId),
                levels,
                bulkCosts);

        check(packet.getStatusId() == RuneStatusConfig.STATUS_POISON,
                "Enhancement payload must describe Poison");
        check(ResourceDefinitions.RUNE_DUST.equals(packet.getResource()),
                "Poison enhancement GUI must receive rune dust from Balance Core");
        check(Math.abs(packet.getPowerStep() - 0.1D) < 0.000001D,
                "Poison enhancement GUI must keep the configured power step");

        int[] receivedLevels = packet.getLevels();
        BigDecimal[][] costs = packet.getCosts();
        check(receivedLevels[1] == 10, "Slot enhancement levels must survive packet construction");
        check(costs[0][0].compareTo(new BigDecimal("19")) == 0, "x1 at global +10 must use level 11 cost");
        check(costs[0][1].compareTo(new BigDecimal("139")) == 0, "x5 at global +10 must use five sequential ZBT costs");
        check(costs[0][2].compareTo(new BigDecimal("458")) == 0, "x10 at global +10 must use ten sequential ZBT costs");
        check(costs[0][3].signum() > 0, "x100 must remain available beyond the former +50 cap");
        check(costs[1][0].compareTo(costs[0][0]) == 0,
                "Every slot row must expose the same collection-global enhancement cost");

        BigDecimal[] afterFifty = enhancementBalance.bulkCosts(statusId, 50, RuneEnhancementPacket.BULK_AMOUNTS);
        check(afterFifty[0].compareTo(new BigDecimal("13684")) == 0,
                "Global +50 must expose level 51 price instead of a limit state");
        for (BigDecimal cost : afterFifty) check(cost.signum() > 0, "Enhancement actions must remain priced after +50");

        receivedLevels[1] = 999;
        costs[0][0] = new BigDecimal("999");
        check(packet.getLevels()[1] == 10, "Level getter must return a defensive copy");
        check(packet.getCosts()[0][0].compareTo(new BigDecimal("19")) == 0, "Cost getter must return a defensive copy");

        check(ClientRuneEnhancements.shouldPulseSync(true, RuneStatusConfig.STATUS_POISON,
                        RuneStatusConfig.STATUS_POISON, false),
                "A real same-status enhancement sync must remain eligible for the green pulse");
        check(!ClientRuneEnhancements.shouldPulseSync(true, RuneStatusConfig.STATUS_POISON,
                        RuneStatusConfig.STATUS_POISON, true),
                "A collection/full synchronization must suppress enhancement pulses");
        check(!ClientRuneEnhancements.shouldPulseSync(true, RuneStatusConfig.STATUS_POISON,
                        RuneStatusConfig.STATUS_POISON + 1, false),
                "A status change must never look like a new enhancement");

        ClientPlayerStats.setCoins(3_000_000_000L);
        ClientPlayerStats.acceptLegacyCoins(Integer.MAX_VALUE);
        check(ClientPlayerStats.getCoins().compareTo(BigDecimal.valueOf(3_000_000_000L)) == 0,
                "Legacy int balance sync must not overwrite a modern large-number coin balance");
        ClientPlayerStats.setCoins(1_000L);
        ClientPlayerStats.acceptLegacyCoins(900);
        check(ClientPlayerStats.getCoins().compareTo(BigDecimal.valueOf(900L)) == 0,
                "Legacy int balance sync must still work while the balance fits the legacy range");

        System.out.println("Rune enhancement packet unlimited-tail and runtime synchronization regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
