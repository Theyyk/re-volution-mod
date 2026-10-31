package com.example.revolution.mob.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
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
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;

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

    public void setGameplayEventSink(GameplayEventSink gameplayEvents) {
        this.gameplayEvents = gameplayEvents == null ? GameplayEventSink.NONE : gameplayEvents;
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
        BigDecimal before = resources.getPlayerResourceValue(player.getUniqueID(), definition.resource);
        if (resources.grantEarnedPlayerResource(player.getUniqueID(), definition.resource, rewardAmount)) {
            BigDecimal newBalance = resources.getPlayerResourceValue(player.getUniqueID(), definition.resource);
            BigDecimal granted = newBalance.subtract(before).max(BigDecimal.ZERO);
            gameplayEvents.factualEvent(new GameplayEvent(player.getUniqueID(), GameplayEventType.MOB_KILL_ANY));
            player.sendMessage(new TextComponentString("§6+" + LargeNumbers.format(granted) + " "
                    + ResourceDefinitions.getDisplayName(definition.resource) + "! Баланс: " + LargeNumbers.format(newBalance)));
            stats.sendTo(player);
        } else {
            RevolutionMod.logger.warn("Skipped invalid mob reward resource: " + definition.resource);
        }
    }

    /** Retains the legacy name-based coin reward while publishing it as confirmed earned coins. */
    public void grantLegacyCoins(EntityPlayerMP player) {
        BigDecimal before = resources.getPlayerResourceValue(player.getUniqueID(), "coins");
        if (!resources.grantEarnedPlayerResource(player.getUniqueID(), "coins", 50)) return;
        BigDecimal after = resources.getPlayerResourceValue(player.getUniqueID(), "coins");
        BigDecimal granted = after.subtract(before).max(BigDecimal.ZERO);
        int balance = resources.getPlayerResource(player.getUniqueID(), "coins");
        player.sendMessage(new TextComponentString("§6+" + LargeNumbers.format(granted) + " монет! Баланс: " + LargeNumbers.format(after)));
        NetworkHandler.sendToClient(new PongPacket(balance), player);
    }
}
