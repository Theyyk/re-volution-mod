package com.example.revolution.client;

import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.Arrays;

final class ClientRuneEnhancements {
    private static final int[] LEVELS = new int[RuneInventory.BOOST_TYPE_COUNT];
    private static final BigDecimal[][] COSTS = new BigDecimal[RuneInventory.BOOST_TYPE_COUNT][RuneEnhancementPacket.BULK_AMOUNTS.length];
    private static int statusId = -1;
    private static String resource = "";
    private static double powerStep;
    private static boolean synced;
    private static boolean suppressNextPulse;

    static { clearCosts(); }
    private ClientRuneEnhancements() {}

    static void suppressNextPulse() { suppressNextPulse = true; }

    static boolean shouldPulseSync(boolean wasSynced, int previousStatusId, int incomingStatusId, boolean suppressPulse) {
        return wasSynced && previousStatusId == incomingStatusId && !suppressPulse;
    }

    static void accept(RuneEnhancementPacket packet) {
        int previousStatusId = statusId;
        int[] previousLevels = Arrays.copyOf(LEVELS, LEVELS.length);
        boolean suppressPulse = suppressNextPulse;
        suppressNextPulse = false;
        boolean mayPulse = packet != null && shouldPulseSync(synced, previousStatusId, packet.getStatusId(), suppressPulse);

        Arrays.fill(LEVELS, 0);
        clearCosts();
        statusId = -1;
        resource = "";
        powerStep = 0.0D;
        if (packet == null) { synced = false; return; }

        statusId = packet.getStatusId();
        resource = packet.getResource() == null ? "" : ResourceDefinitions.normalize(packet.getResource());
        powerStep = Math.max(0.0D, packet.getPowerStep());

        int[] incomingLevels = packet.getLevels();
        BigDecimal[][] incomingCosts = packet.getCosts();
        int limit = Math.min(LEVELS.length, incomingLevels.length);
        for (int rune = 0; rune < limit; rune++) {
            LEVELS[rune] = Math.max(0, incomingLevels[rune]);
            if (rune >= incomingCosts.length || incomingCosts[rune] == null) continue;
            int amountLimit = Math.min(COSTS[rune].length, incomingCosts[rune].length);
            for (int amount = 0; amount < amountLimit; amount++) {
                BigDecimal value = incomingCosts[rune][amount];
                COSTS[rune][amount] = value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
            }
        }

        if (mayPulse) {
            for (int rune = 0; rune < LEVELS.length; rune++) {
                if (LEVELS[rune] > previousLevels[rune]) RuneUxUiHandler.noteEnhancement(rune);
            }
        }
        synced = true;
    }

    static int levelOf(int runeIndex) {
        if (runeIndex < 0 || runeIndex >= LEVELS.length) return 0;
        return LEVELS[runeIndex];
    }

    static BigDecimal costOf(int runeIndex, int bulkAmount) {
        if (runeIndex < 0 || runeIndex >= COSTS.length) return BigDecimal.ZERO;
        for (int i = 0; i < RuneEnhancementPacket.BULK_AMOUNTS.length; i++) {
            if (RuneEnhancementPacket.BULK_AMOUNTS[i] == bulkAmount) return COSTS[runeIndex][i];
        }
        return BigDecimal.ZERO;
    }

    static int getStatusId() { return statusId; }
    static String getResource() { return resource; }
    static double getPowerStep() { return powerStep; }

    static double multiplierOf(int runeIndex) {
        return 1.0D + levelOf(runeIndex) * powerStep;
    }

    private static void clearCosts() {
        for (BigDecimal[] runeCosts : COSTS) Arrays.fill(runeCosts, BigDecimal.ZERO);
    }
}
