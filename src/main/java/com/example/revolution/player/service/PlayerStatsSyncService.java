package com.example.revolution.player.service;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PlayerStatsPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneEconomyConfig;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCollectionProgression;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.service.RuneCombatService;
import com.example.revolution.rune.service.RunePurchaseBalanceService;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;

public final class PlayerStatsSyncService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsService playerStats;
    private final RuneCombatService combat;
    private final RunePurchaseBalanceService purchaseBalance;

    public PlayerStatsSyncService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsService playerStats,
            RuneCombatService combat) {
        this(players, resources, playerStats, combat, null);
    }

    public PlayerStatsSyncService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsService playerStats,
            RuneCombatService combat,
            RunePurchaseBalanceService purchaseBalance) {
        this.players = players;
        this.resources = resources;
        this.playerStats = playerStats;
        this.combat = combat;
        this.purchaseBalance = purchaseBalance;
    }

    public void sendTo(EntityPlayerMP player) {
        if (!NetworkHandler.clientHasRevolutionMod(player)) return;
        UUID uuid = player.getUniqueID();
        BigDecimal coins = resources.getPlayerResourceValue(uuid, "coins");
        BigDecimal crystals = resources.getPlayerResourceValue(uuid, "crystals");
        BigDecimal runeDust = resources.getPlayerResourceValue(uuid, ResourceDefinitions.RUNE_DUST);
        BigDecimal runeShards = resources.getPlayerResourceValue(uuid, ResourceDefinitions.RUNE_SHARDS);
        BigDecimal runeCores = resources.getPlayerResourceValue(uuid, ResourceDefinitions.RUNE_CORES);
        int totalDamage = playerStats.getTotalDamageBonus(uuid);
        int activeCollection = players.getActiveCollection(uuid);
        int statusId = RuneStatusConfig.statusForCollection(activeCollection);

        RuneCombatMath.Snapshot stableCombat = combat.resolveStable(uuid);
        double baseDamage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        double resolvedClickDamage = RuneCombatMath.clickDamage(baseDamage, stableCombat, false);
        double resolvedPoisonDamage = RuneCombatMath.poisonDamage(stableCombat);

        String purchaseResource = RuneEconomyConfig.purchaseResourceForStatus(statusId);
        String canonicalPurchaseResource = ResourceDefinitions.findCanonicalName(purchaseResource);
        if (canonicalPurchaseResource == null) canonicalPurchaseResource = "coins";
        int purchaseUnitCost = RuneEconomyConfig.purchaseUnitCost(statusId);
        if (purchaseBalance != null && RuneStatusConfig.isRuneStatus(statusId)) {
            RuneCollectionProgression progression = purchaseProgression(uuid, activeCollection);
            canonicalPurchaseResource = purchaseBalance.purchaseResource(statusId);
            purchaseUnitCost = purchaseBalance.purchaseCost(
                    statusId, progression.stageRank(), progression.progressionIndex());
        }

        String awakeningResource = RuneEconomyConfig.awakeningResourceForStatus(statusId);
        String canonicalAwakeningResource = ResourceDefinitions.findCanonicalName(awakeningResource);
        if (canonicalAwakeningResource == null) canonicalAwakeningResource = awakeningResource;
        int awakeningNormalCost = RuneEconomyConfig.awakeningCost(statusId, RuneInventory.NORMAL_RANK);
        int awakeningImprovedCost = RuneEconomyConfig.awakeningCost(statusId, RuneInventory.IMPROVED_RANK);

        NetworkHandler.sendToClient(new PlayerStatsPacket(coins, crystals, runeDust, runeShards, runeCores, totalDamage,
                statusId, canonicalPurchaseResource, purchaseUnitCost, canonicalAwakeningResource, awakeningNormalCost,
                awakeningImprovedCost, resolvedClickDamage, resolvedPoisonDamage), player);
    }

    private RuneCollectionProgression purchaseProgression(UUID uuid, int collectionIndex) {
        int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (RuneRecord rune : players.getRunesInCollection(uuid, collectionIndex)) {
            if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
            bestRanks[rune.runeIndex] = Math.max(
                    bestRanks[rune.runeIndex], RuneProgression.clampRank(rune.layer));
        }
        return RuneCollectionProgression.fromBestRanks(bestRanks);
    }
}
