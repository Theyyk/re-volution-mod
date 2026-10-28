package com.example.revolution.rune.service;

import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
import java.util.ArrayList;
import java.util.List;

/** Maps application values to wire DTOs without owning gameplay configuration. */
public final class RunePacketFactory {
    private RunePacketFactory() {}

    public static RuneEnhancementPacket enhancements(
            int statusId,
            String resource,
            double powerStep,
            int[] levels,
            int[] bulkCosts) {
        return new RuneEnhancementPacket(
                statusId,
                resource,
                powerStep,
                levels,
                buildCosts(bulkCosts));
    }

    public static RuneCollectionsPacket collections(List<String> storedNames, int activeCollection) {
        List<String> displayNames = new ArrayList<>();
        for (int i = 0; i < storedNames.size(); i++) {
            displayNames.add(com.example.revolution.rune.config.RuneStatusConfig.displayNameForCollection(i, storedNames.get(i)));
        }
        return new RuneCollectionsPacket(displayNames, activeCollection);
    }

    /** Wire format remains per-slot for compatibility, but global enhancement pricing is identical for every row. */
    private static int[][] buildCosts(int[] bulkCosts) {
        int[][] result = new int[RuneLayoutDefinition.BOOST_TYPE_COUNT][RuneEnhancementPacket.BULK_AMOUNTS.length];
        for (int rune = 0; rune < result.length; rune++) {
            if (bulkCosts == null) continue;
            int limit = Math.min(result[rune].length, bulkCosts.length);
            for (int amount = 0; amount < limit; amount++) {
                result[rune][amount] = Math.max(0, bulkCosts[amount]);
            }
        }
        return result;
    }
}
