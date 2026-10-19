package com.example.revolution.player.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.config.RuneEconomyConfig;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import java.util.UUID;

public final class PlayerStatsService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;

    public PlayerStatsService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        this.players = players;
        this.enhancements = enhancements;
    }

    public int getTotalDamageBonus(UUID uuid) {
        double value = getTotalDamageBonusValue(uuid);
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(Math.max(0.0D, value));
    }

    public double getTotalDamageBonusValue(UUID uuid) {
        double totalBonus = 0.0D;
        int collectionCount = players.getCollectionNames(uuid).size();
        for (int collectionIndex = 0; collectionIndex < collectionCount; collectionIndex++) {
            List<RuneRecord> runes = players.getRunesInCollection(uuid, collectionIndex);
            int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
            if (!RuneStatusConfig.isRuneStatus(statusId)) { totalBonus += runes.size(); continue; }
            int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
            for (RuneRecord rune : runes) {
                if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
                bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], rune.layer);
            }
            int[] levels = enhancements.getLevels(uuid, collectionIndex);
            for (int rune = 0; rune < bestRanks.length; rune++) {
                int rank = bestRanks[rune]; if (rank <= 0) continue;
                totalBonus += rankMultiplier(rank) * RuneEconomyConfig.enhancementPowerMultiplier(statusId, levels[rune]);
            }
        }
        return totalBonus;
    }

    private int rankMultiplier(int rank) {
        if (rank >= RuneInventory.ADVANCED_RANK) return 9;
        if (rank >= RuneInventory.IMPROVED_RANK) return 3;
        return 1;
    }

}
