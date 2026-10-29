package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RunePrestigeResetRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Applies the Prestige rank policy to factual rune state. Enhancement storage is deliberately untouched. */
public final class RunePrestigeResetService {
    private final PlayerRepository players;

    public RunePrestigeResetService(PlayerRepository players) {
        if (players == null) throw new IllegalArgumentException("player repository is required");
        this.players = players;
    }

    public void reset(UUID playerUuid) {
        Set<RuneCollectionId> unlocked = unlockedCollections(playerUuid);
        for (RuneCollectionId collectionId : RuneCollectionId.values()) {
            if (!unlocked.contains(collectionId)) continue;
            resetCollection(playerUuid, collectionId.legacyIndex());
        }
    }

    private void resetCollection(UUID playerUuid, int collectionIndex) {
        List<RuneRecord> records = players.getRunesInCollection(playerUuid, collectionIndex);
        Set<Integer> processedRuneIndexes = new HashSet<>();
        for (RuneRecord record : records) {
            if (!processedRuneIndexes.add(record.runeIndex)) continue;
            int bestRank = bestRank(records, record.runeIndex);
            int targetRank = RunePrestigeResetRules.rankAfterReset(bestRank);
            if (targetRank != bestRank) {
                players.setRuneRank(playerUuid, collectionIndex, record.runeIndex, targetRank);
            }
        }
    }

    private Set<RuneCollectionId> unlockedCollections(UUID playerUuid) {
        if (players instanceof RuneCollectionStateRepository) {
            return ((RuneCollectionStateRepository) players).getUnlockedCollections(playerUuid);
        }
        java.util.EnumSet<RuneCollectionId> all = java.util.EnumSet.noneOf(RuneCollectionId.class);
        int count = Math.min(players.getCollectionNames(playerUuid).size(), RuneCollectionId.values().length);
        for (int i = 0; i < count; i++) {
            RuneCollectionId id = RuneCollectionId.fromLegacyIndex(i);
            if (id != null) all.add(id);
        }
        if (all.isEmpty()) all.add(RuneCollectionId.COLLECTION_01);
        return all;
    }

    private static int bestRank(List<RuneRecord> records, int runeIndex) {
        int best = 0;
        for (RuneRecord record : records) {
            if (record.runeIndex == runeIndex && record.layer > best) best = record.layer;
        }
        return best;
    }
}
