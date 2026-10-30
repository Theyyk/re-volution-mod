package com.example.revolution.rune.service;

import com.example.revolution.network.RuneCollectionMetadataPacket;
import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Maps application values to wire DTOs without owning gameplay configuration. */
public final class RunePacketFactory {
    private RunePacketFactory() {}

    public static RuneEnhancementPacket enhancements(
            int statusId,
            String resource,
            double powerStep,
            int[] levels,
            BigDecimal[] bulkCosts) {
        return new RuneEnhancementPacket(statusId, resource, powerStep, levels, buildCosts(bulkCosts));
    }

    public static RuneCollectionsPacket collections(List<String> storedNames, int activeCollection) {
        List<String> displayNames = new ArrayList<>();
        for (int i = 0; i < storedNames.size(); i++) {
            displayNames.add(RuneStatusConfig.displayNameForCollection(i, storedNames.get(i)));
        }
        return new RuneCollectionsPacket(displayNames, activeCollection);
    }

    public static RuneCollectionMetadataPacket collectionMetadata(
            List<String> storedNames,
            int activeCollection,
            Set<RuneCollectionId> unlocked,
            List<Integer> improvedOrHigherProgress,
            int unlockThreshold) {
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int unlockedMask = 0;
        for (RuneCollectionId collection : RuneCollectionId.values()) {
            int index = collection.legacyIndex();
            ids.add(collection.id());
            String stored = index < storedNames.size() ? storedNames.get(index) : technicalCollectionName(index);
            names.add(RuneStatusConfig.displayNameForCollection(index, stored));
            if (unlocked.contains(collection)) unlockedMask |= 1 << index;
        }
        return new RuneCollectionMetadataPacket(
                ids, names, unlockedMask, activeCollection, improvedOrHigherProgress, unlockThreshold);
    }

    private static String technicalCollectionName(int index) {
        int ordinal = index + 1;
        return ordinal < 10 ? "Collection 0" + ordinal : "Collection " + ordinal;
    }

    /** Wire format remains per-slot for compatibility, but global enhancement pricing is identical for every row. */
    private static BigDecimal[][] buildCosts(BigDecimal[] bulkCosts) {
        BigDecimal[][] result = new BigDecimal[RuneLayoutDefinition.BOOST_TYPE_COUNT][RuneEnhancementPacket.BULK_AMOUNTS.length];
        for (int rune = 0; rune < result.length; rune++) {
            for (int amount = 0; amount < result[rune].length; amount++) result[rune][amount] = BigDecimal.ZERO;
            if (bulkCosts == null) continue;
            int limit = Math.min(result[rune].length, bulkCosts.length);
            for (int amount = 0; amount < limit; amount++) {
                BigDecimal value = bulkCosts[amount];
                result[rune][amount] = value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
            }
        }
        return result;
    }
}
