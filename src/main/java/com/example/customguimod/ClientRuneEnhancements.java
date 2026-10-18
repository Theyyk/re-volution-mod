package com.example.customguimod;

import com.example.customguimod.resource.ResourceManager;

import java.util.Arrays;

final class ClientRuneEnhancements {
    private static final int[] LEVELS = new int[RuneInventory.BOOST_TYPE_COUNT];
    private static final int[][] COSTS = new int[RuneInventory.BOOST_TYPE_COUNT][RuneEnhancementPacket.BULK_AMOUNTS.length];
    private static int statusId = -1;
    private static String resource = "";
    private static double powerStep;
    private static boolean synced;

    private ClientRuneEnhancements() {}

    static void accept(RuneEnhancementPacket packet) {
        int previousStatusId = statusId;
        int[] previousLevels = Arrays.copyOf(LEVELS, LEVELS.length);
        boolean mayPulse = synced && packet != null && packet.getStatusId() == previousStatusId;

        Arrays.fill(LEVELS, 0);
        for (int[] runeCosts : COSTS) Arrays.fill(runeCosts, 0);
        statusId = -1;
        resource = "";
        powerStep = 0.0D;
        if (packet == null) {
            synced = false;
            return;
        }

        statusId = packet.getStatusId();
        resource = packet.getResource() == null ? "" : ResourceManager.normalize(packet.getResource());
        powerStep = Math.max(0.0D, packet.getPowerStep());

        int[] incomingLevels = packet.getLevels();
        int[][] incomingCosts = packet.getCosts();
        int limit = Math.min(LEVELS.length, incomingLevels.length);
        for (int rune = 0; rune < limit; rune++) {
            LEVELS[rune] = Math.max(0, incomingLevels[rune]);
            if (rune >= incomingCosts.length || incomingCosts[rune] == null) continue;
            int amountLimit = Math.min(COSTS[rune].length, incomingCosts[rune].length);
            for (int amount = 0; amount < amountLimit; amount++) {
                COSTS[rune][amount] = Math.max(0, incomingCosts[rune][amount]);
            }
        }

        if (mayPulse) {
            for (int rune = 0; rune < LEVELS.length; rune++) {
                if (LEVELS[rune] > previousLevels[rune]) {
                    RuneUxUiHandler.noteEnhancement(rune);
                }
            }
        }
        synced = true;
    }

    static int levelOf(int runeIndex) {
        if (runeIndex < 0 || runeIndex >= LEVELS.length) return 0;
        return LEVELS[runeIndex];
    }

    static int costOf(int runeIndex, int bulkAmount) {
        if (runeIndex < 0 || runeIndex >= COSTS.length) return 0;
        for (int i = 0; i < RuneEnhancementPacket.BULK_AMOUNTS.length; i++) {
            if (RuneEnhancementPacket.BULK_AMOUNTS[i] == bulkAmount) return COSTS[runeIndex][i];
        }
        return 0;
    }

    static int getStatusId() { return statusId; }
    static String getResource() { return resource; }
    static double getPowerStep() { return powerStep; }

    static double multiplierOf(int runeIndex) {
        return 1.0D + levelOf(runeIndex) * powerStep;
    }
}
