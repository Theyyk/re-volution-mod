package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.PrimaryRuneCatalog;
import com.example.revolution.rune.definition.RuneStat;
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

    public RuneRewardBonusService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        this.players = players;
        this.enhancements = enhancements;
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
