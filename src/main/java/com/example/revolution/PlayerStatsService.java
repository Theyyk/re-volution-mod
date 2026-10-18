package com.example.revolution;


import com.example.revolution.rune.RuneCombatMath;
import com.example.revolution.rune.RuneCombatService;
import com.example.revolution.rune.RuneEconomyConfig;
import com.example.revolution.rune.RuneEnhancementManager;
import com.example.revolution.rune.RuneInventory;
import com.example.revolution.rune.RuneStatusRegistry;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PlayerStatsPacket;
import com.example.revolution.persistence.MongoManager;
import com.example.revolution.resource.ResourceManager;

import net.minecraft.entity.SharedMonsterAttributes;
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
        int collectionCount = MongoManager.getCollectionNames(uuid).size();
        for (int collectionIndex = 0; collectionIndex < collectionCount; collectionIndex++) {
            List<MongoManager.SavedRune> runes = MongoManager.getRunesInCollection(uuid, collectionIndex);
            int statusId = RuneStatusRegistry.statusForCollection(collectionIndex);
            if (!RuneStatusRegistry.isRuneStatus(statusId)) { totalBonus += runes.size(); continue; }
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (MongoManager.SavedRune rune : runes) {
                if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
            }
            int[] enhancements = RuneEnhancementManager.getLevels(uuid, collectionIndex);
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
        if (!NetworkHandler.clientHasRevolutionMod(player)) return;
        UUID uuid = player.getUniqueID();
        BigDecimal coins = ResourceManager.getPlayerResourceValue(uuid, "coins");
        BigDecimal crystals = ResourceManager.getPlayerResourceValue(uuid, "crystals");
        BigDecimal runeDust = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_DUST);
        BigDecimal runeShards = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_SHARDS);
        BigDecimal runeCores = ResourceManager.getPlayerResourceValue(uuid, ResourceManager.RUNE_CORES);
        int totalDamage = getTotalDamageBonus(uuid);
        int activeCollection = MongoManager.getActiveCollection(uuid);
        int statusId = RuneStatusRegistry.statusForCollection(activeCollection);

        RuneCombatMath.Snapshot stableCombat = RuneCombatService.resolveStable(uuid);
        double baseDamage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        double resolvedClickDamage = RuneCombatMath.clickDamage(baseDamage, stableCombat, false);
        double resolvedPoisonDamage = RuneCombatMath.poisonDamage(stableCombat);

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
                statusId, canonicalPurchaseResource, purchaseUnitCost, canonicalAwakeningResource, awakeningNormalCost,
                awakeningImprovedCost, resolvedClickDamage, resolvedPoisonDamage), player);
    }
}
