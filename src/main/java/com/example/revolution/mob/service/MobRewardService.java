package com.example.revolution.mob.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.mob.definition.ManagedMobDefinition;
import com.example.revolution.mob.rules.MobContributionRules;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PongPacket;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.service.RuneMobEffectService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class MobRewardService {
    private final ResourceService resources;
    private final RuneMobEffectService runeEffects;
    private final PlayerStatsSyncService stats;
    private final PlayerRepository players;

    public MobRewardService(
            ResourceService resources,
            RuneMobEffectService runeEffects,
            PlayerStatsSyncService stats,
            PlayerRepository players) {
        this.resources = resources;
        this.runeEffects = runeEffects;
        this.stats = stats;
        this.players = players;
    }

    public void grant(EntityPlayerMP player, ManagedMobDefinition definition) {
        grantResolved(player, definition, definition.amount);
    }

    public void grantProportional(
            EntityPlayerMP player,
            ManagedMobDefinition definition,
            BigDecimal contribution,
            BigDecimal totalContribution) {
        BigDecimal baseShare = MobContributionRules.proportionalReward(
                definition.amount, contribution, totalContribution);
        if (baseShare.signum() <= 0) return;
        grantResolved(player, definition, baseShare);
    }

    private void grantResolved(EntityPlayerMP player, ManagedMobDefinition definition, BigDecimal baseReward) {
        BigDecimal rewardAmount = runeEffects.rewardAmount(player.getUniqueID(), definition.resource, baseReward);
        if (resources.grantEarnedPlayerResource(player.getUniqueID(), definition.resource, rewardAmount)) {
            BigDecimal newBalance = resources.getPlayerResourceValue(player.getUniqueID(), definition.resource);
            player.sendMessage(new TextComponentString("§6+" + LargeNumbers.format(rewardAmount) + " "
                    + ResourceDefinitions.getDisplayName(definition.resource) + "! Баланс: " + LargeNumbers.format(newBalance)));
            stats.sendTo(player);
        } else {
            RevolutionMod.logger.warn("Skipped invalid mob reward resource: " + definition.resource);
        }
    }

    /** Retains the legacy name-based 50-coin reward while publishing it as confirmed earned coins. */
    public void grantLegacyCoins(EntityPlayerMP player) {
        if (!resources.grantEarnedPlayerResource(player.getUniqueID(), "coins", 50)) return;
        int balance = resources.getPlayerResource(player.getUniqueID(), "coins");
        player.sendMessage(new TextComponentString("§6+50 монет! Баланс: " + balance));
        NetworkHandler.sendToClient(new PongPacket(balance), player);
    }
}
