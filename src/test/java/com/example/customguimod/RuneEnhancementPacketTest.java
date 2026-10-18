package com.example.customguimod;

import com.example.customguimod.resource.ResourceManager;

import java.math.BigDecimal;

final class RuneEnhancementPacketTest {
    public static void main(String[] args) {
        int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        levels[0] = 0;
        levels[1] = 10;

        RuneEnhancementPacket packet = new RuneEnhancementPacket(levels);

        check(packet.getStatusId() == RuneStatusRegistry.STATUS_POISON,
                "v1.2.9 compatibility payload must describe Poison");
        check(ResourceManager.RUNE_DUST.equals(packet.getResource()),
                "Poison enhancement GUI must receive rune dust from the server");
        check(Math.abs(packet.getPowerStep() - 0.1D) < 0.000001D,
                "Poison enhancement GUI must receive the configured power step");

        int[] receivedLevels = packet.getLevels();
        int[][] costs = packet.getCosts();
        check(receivedLevels[1] == 10, "Slot enhancement levels must survive packet construction");
        check(costs[0][0] == 10, "x1 cost at level 0 must be 10 rune dust");
        check(costs[0][1] == 50, "x5 cost must be the sequential five-level total");
        check(costs[0][2] == 100, "x10 cost must be the sequential ten-level total");
        check(costs[0][3] == 1000, "x100 cost must be the sequential hundred-level total");
        check(costs[1][0] == RuneEconomyConfig.enhancementCost(
                        RuneStatusRegistry.STATUS_POISON, 10, 1),
                "Each slot cost must be calculated from that slot's current level");

        receivedLevels[1] = 999;
        costs[0][0] = 999;
        check(packet.getLevels()[1] == 10, "Level getter must return a defensive copy");
        check(packet.getCosts()[0][0] == 10, "Cost getter must return a defensive copy");

        check(ClientRuneEnhancements.shouldPulseSync(true, RuneStatusRegistry.STATUS_POISON,
                        RuneStatusRegistry.STATUS_POISON, false),
                "A real same-status enhancement sync must remain eligible for the green pulse");
        check(!ClientRuneEnhancements.shouldPulseSync(true, RuneStatusRegistry.STATUS_POISON,
                        RuneStatusRegistry.STATUS_POISON, true),
                "A collection/full synchronization must suppress enhancement pulses");
        check(!ClientRuneEnhancements.shouldPulseSync(true, RuneStatusRegistry.STATUS_POISON,
                        RuneStatusRegistry.STATUS_POISON + 1, false),
                "A status change must never look like a new enhancement");

        ClientPlayerStats.setCoins(3_000_000_000L);
        ClientPlayerStats.acceptLegacyCoins(Integer.MAX_VALUE);
        check(ClientPlayerStats.getCoins().compareTo(BigDecimal.valueOf(3_000_000_000L)) == 0,
                "Legacy int balance sync must not overwrite a modern large-number coin balance");
        ClientPlayerStats.setCoins(1_000L);
        ClientPlayerStats.acceptLegacyCoins(900);
        check(ClientPlayerStats.getCoins().compareTo(BigDecimal.valueOf(900L)) == 0,
                "Legacy int balance sync must still work while the balance fits the legacy range");

        System.out.println("Rune enhancement packet and runtime synchronization regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
