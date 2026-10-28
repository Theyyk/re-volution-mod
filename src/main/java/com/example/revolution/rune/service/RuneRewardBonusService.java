package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.MobRewardBonus;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import java.util.UUID;

/** Resolves server-authoritative rune reward stats for the player's active rune deck. */
public final class RuneRewardBonusService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;
    private final RuneEffectBalanceService effectBalance;

    public RuneRewardBonusService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        this(players, enhancements, new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults()));
    }

    public RuneRewardBonusService(
            PlayerRepository players,
            RuneEnhancementRepository enhancements,
            RuneEffectBalanceService effectBalance) {
        if (players == null || enhancements == null || effectBalance == null) {
            throw new IllegalArgumentException("rune reward dependencies are required");
        }
        this.players = players;
        this.enhancements = enhancements;
        this.effectBalance = effectBalance;
    }

    public double percentFor(UUID uuid, String resource) {
        RuneStatType targetType = MobRewardBonus.statTypeFor(resource);
        if (uuid == null || targetType == null) return 0.0D;

        int collectionIndex = players.getActiveCollection(uuid);
        if (RuneStatusConfig.statusForCollection(collectionIndex) != RuneStatusConfig.STATUS_POISON) return 0.0D;

        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> runes = players.getRunesInCollection(uuid, collectionIndex);
        for (RuneRecord rune : runes) {
            if (rune == null) continue;
            inventory.set(rune.slot, rune.runeIndex, rune.layer);
        }

        int[] enhancementLevels = enhancements.getLevels(uuid, collectionIndex);
        double total = 0.0D;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            RuneInventory.Entry owned = inventory.getBoost(rune);
            if (owned == null) continue;
            int enhancementLevel = enhancementLevels != null && rune < enhancementLevels.length
                    ? Math.max(0, enhancementLevels[rune]) : 0;
            for (RuneEffectBalanceService.ResolvedEffect effect : effectBalance.effects(
                    RuneStatusConfig.STATUS_POISON, rune, owned.layer, enhancementLevel)) {
                if (effect.type != RuneEffectBalanceService.EffectType.STAT || effect.statType != targetType) continue;
                total += effect.primaryValue;
            }
        }
        return Double.isFinite(total) ? Math.max(0.0D, total) : 0.0D;
    }
}
