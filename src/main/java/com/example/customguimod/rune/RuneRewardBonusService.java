package com.example.customguimod.rune;
import com.example.customguimod.persistence.MongoManager;

import java.util.List;
import java.util.UUID;

/** Resolves server-authoritative rune reward stats for the player's active rune deck. */
final class RuneRewardBonusService {
    private RuneRewardBonusService() {}

    static double percentFor(UUID uuid, String resource) {
        RuneStatType targetType = MobRewardBonus.statTypeFor(resource);
        if (uuid == null || targetType == null) return 0.0D;

        int collectionIndex = MongoManager.getActiveCollection(uuid);
        if (RuneStatusRegistry.statusForCollection(collectionIndex) != RuneStatusRegistry.STATUS_POISON) return 0.0D;

        RuneInventory inventory = new RuneInventory();
        List<MongoManager.SavedRune> runes = MongoManager.getRunesInCollection(uuid, collectionIndex);
        for (MongoManager.SavedRune rune : runes) {
            if (rune == null) continue;
            inventory.set(rune.slot, rune.runeIndex, rune.layer);
        }

        int[] enhancementLevels = RuneEnhancementManager.getLevels(uuid, collectionIndex);
        double total = 0.0D;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;

            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
            if (definition == null || definition.stats == null) continue;

            int enhancementLevel = rune < enhancementLevels.length ? Math.max(0, enhancementLevels[rune]) : 0;
            double enhancementMultiplier = 1.0D + enhancementLevel * 0.1D;
            for (RuneStat stat : definition.stats) {
                if (stat == null || stat.type != targetType) continue;
                total += stat.resolve(owned.layer, enhancementMultiplier).finalValue;
            }
        }
        return Double.isFinite(total) ? Math.max(0.0D, total) : 0.0D;
    }
}
