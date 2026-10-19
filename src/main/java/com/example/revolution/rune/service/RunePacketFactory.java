package com.example.revolution.rune.service;

import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.rune.config.RuneEconomyConfig;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
import java.util.ArrayList;
import java.util.List;

/** Maps application values to wire DTOs. Historical Poison metadata stays unchanged in 1.3.1. */
public final class RunePacketFactory {
    private RunePacketFactory() {}

    public static RuneEnhancementPacket enhancements(int[] levels) {
        int status = RuneStatusConfig.STATUS_POISON;
        return new RuneEnhancementPacket(status, RuneEconomyConfig.enhancementResourceForStatus(status),
                RuneEconomyConfig.enhancementPowerStep(status), levels, buildCosts(status, levels));
    }

    public static RuneCollectionsPacket collections(List<String> storedNames, int activeCollection) {
        List<String> displayNames = new ArrayList<>();
        for (int i = 0; i < storedNames.size(); i++) {
            displayNames.add(RuneStatusConfig.displayNameForCollection(i, storedNames.get(i)));
        }
        return new RuneCollectionsPacket(displayNames, activeCollection);
    }
    private static int[][] buildCosts(int statusId, int[] levels) {
        int[][] result = new int[RuneLayoutDefinition.BOOST_TYPE_COUNT][RuneEnhancementPacket.BULK_AMOUNTS.length];
        for (int rune = 0; rune < result.length; rune++) {
            int currentLevel = levels != null && rune < levels.length ? Math.max(0, levels[rune]) : 0;
            for (int amount = 0; amount < RuneEnhancementPacket.BULK_AMOUNTS.length; amount++) {
                result[rune][amount] = RuneEconomyConfig.enhancementCost(
                        statusId,
                        currentLevel,
                        RuneEnhancementPacket.BULK_AMOUNTS[amount]
                );
            }
        }
        return result;
    }
}
