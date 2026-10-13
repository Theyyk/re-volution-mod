package com.example.customguimod;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.UUID;

public final class PlayerStatsService {

    private PlayerStatsService() {}

    public static int getTotalDamageBonus(UUID uuid) {
        int totalBonus = 0;
        int deckCount = MongoManager.getDeckNames(uuid).size();

        for (int deckIndex = 0; deckIndex < deckCount; deckIndex++) {
            List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(uuid, deckIndex);
            int statusId = RuneStatusRegistry.statusForDeck(deckIndex);

            if (!RuneStatusRegistry.isRuneStatus(statusId)) {
                totalBonus += cards.size();
                continue;
            }

            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (MongoManager.SavedCard card : cards) {
                if (card.cardIndex < 0 || card.cardIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                bestRanks[card.cardIndex] = Math.max(bestRanks[card.cardIndex], card.layer);
            }

            for (int rank : bestRanks) {
                if (rank <= 0) continue;
                totalBonus += rankMultiplier(rank);
            }
        }

        return totalBonus;
    }

    private static int rankMultiplier(int rank) {
        if (rank >= RuneInventory.ADVANCED_RANK) return 9;
        if (rank >= RuneInventory.IMPROVED_RANK) return 3;
        return 1;
    }

    public static void sendTo(EntityPlayerMP player) {
        UUID uuid = player.getUniqueID();
        int coins = MongoManager.getBalance(uuid);
        int crystals = MongoManager.getCrystals(uuid);
        int runeDust = ResourceManager.getPlayerResource(uuid, ResourceManager.RUNE_DUST);
        int runeShards = ResourceManager.getPlayerResource(uuid, ResourceManager.RUNE_SHARDS);
        int runeCores = ResourceManager.getPlayerResource(uuid, ResourceManager.RUNE_CORES);
        int totalDamage = getTotalDamageBonus(uuid);
        int activeDeck = MongoManager.getActiveDeck(uuid);
        int statusId = RuneStatusRegistry.statusForDeck(activeDeck);

        String purchaseResource = RuneEconomyConfig.purchaseResourceForStatus(statusId);
        String canonicalPurchaseResource = ResourceManager.findCanonicalName(purchaseResource);
        if (canonicalPurchaseResource == null) canonicalPurchaseResource = "coins";
        int purchaseUnitCost = RuneEconomyConfig.purchaseUnitCost(statusId);

        String awakeningResource = RuneEconomyConfig.awakeningResourceForStatus(statusId);
        String canonicalAwakeningResource = ResourceManager.findCanonicalName(awakeningResource);
        if (canonicalAwakeningResource == null) canonicalAwakeningResource = awakeningResource;
        int awakeningNormalCost = RuneEconomyConfig.awakeningCost(statusId, RuneInventory.NORMAL_RANK);
        int awakeningImprovedCost = RuneEconomyConfig.awakeningCost(statusId, RuneInventory.IMPROVED_RANK);

        NetworkHandler.INSTANCE.sendTo(
                new PlayerStatsPacket(
                        coins, crystals, runeDust, runeShards, runeCores, totalDamage,
                        statusId,
                        canonicalPurchaseResource, purchaseUnitCost,
                        canonicalAwakeningResource, awakeningNormalCost, awakeningImprovedCost
                ),
                player
        );
    }
}
