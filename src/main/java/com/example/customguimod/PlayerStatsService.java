package com.example.customguimod;

import net.minecraft.entity.player.EntityPlayerMP;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class PlayerStatsService {
    private PlayerStatsService() {}

    public static int getTotalDamageBonus(UUID uuid) {
        double value = getTotalDamageBonusValue(uuid);
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(Math.max(0.0D, value));
    }

    public static double getTotalDamageBonusValue(UUID uuid) {
        double totalBonus = 0.0D;
        int deckCount = MongoManager.getDeckNames(uuid).size();
        for (int deckIndex = 0; deckIndex < deckCount; deckIndex++) {
            List<MongoManager.SavedCard> cards = MongoManager.getCardsInDeck(uuid, deckIndex);
            int statusId = RuneStatusRegistry.statusForDeck(deckIndex);
            if (!RuneStatusRegistry.isRuneStatus(statusId)) { totalBonus += cards.size(); continue; }
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (MongoManager.SavedCard card : cards) {
                if (card.cardIndex < 0 || card.cardIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                bestRanks[card.cardIndex] = Math.max(bestRanks[card.cardIndex], card.layer);
            }
            int[] enhancements = RuneEnhancementManager.getLevels(uuid, deckIndex);
            for (int rune = 0; rune < bestRanks.length; rune++) {
                int rank = bestRanks[rune]; if (rank <= 0) continue;
                totalBonus += rankMultiplier(rank) * RuneEconomyConfig.enhancementPowerMultiplier(statusId, enhancements[rune]);
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
        if (!NetworkHandler.clientHasCustomGuiMod(player)) return;
        UUID uuid = player.getUniqueID();
        BigDecimal coins = ResourceManager.getPlayerResourceValue(uuid, "coins");
        BigDecimal crystals = ResourceManager.getPlayerResourceValue(uuid, "crystals");
        BigDecimal runeDust = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_DUST);
        BigDecimal runeShards = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_SHARDS);
        BigDecimal runeCores = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_CORES);
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

        NetworkHandler.sendToClient(new PlayerStatsPacket(coins, crystals, runeDust, runeShards, runeCores, totalDamage,
                statusId, canonicalPurchaseResource, purchaseUnitCost, canonicalAwakeningResource, awakeningNormalCost, awakeningImprovedCost), player);
    }
}
