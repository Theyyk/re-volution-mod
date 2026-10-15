package com.example.customguimod;

import java.util.List;
import java.util.UUID;

/** Resolves server-authoritative rune reward stats for the player's active rune deck. */
final class RuneRewardBonusService {
    private RuneRewardBonusService() {}

    static double percentFor(UUID uuid, String resource) {
        RuneStatType targetType = MobRewardBonus.statTypeFor(resource);
        if (uuid == null || targetType == null) return 0.0D;

        int deckIndex = MongoManager.getActiveDeck(uuid);
        if (RuneStatusRegistry.statusForDeck(deckIndex) != RuneStatusRegistry.STATUS_POISON) return 0.0D;

        RuneInventory inventory = new RuneInventory();
        List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(uuid, deckIndex);
        for (MongoManager.SavedCard card : cards) {
            if (card == null) continue;
            inventory.set(card.slot, card.cardIndex, card.layer);
        }

        int[] enhancementLevels = RuneEnhancementManager.getLevels(uuid, deckIndex);
        double total = 0.0D;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;

            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(rune);
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
