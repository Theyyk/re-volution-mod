package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RunePrestigeResetRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Applies the Prestige selective-wipe policy to factual rune state. */
public final class RunePrestigeResetService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;

    public RunePrestigeResetService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        if (players == null) throw new IllegalArgumentException("player repository is required");
        if (enhancements == null) throw new IllegalArgumentException("enhancement repository is required");
        this.players = players;
        this.enhancements = enhancements;
    }

    public void reset(UUID playerUuid) {
        Set<RuneCollectionId> unlocked = unlockedCollections(playerUuid);
        for (RuneCollectionId collectionId : RuneCollectionId.values()) {
            if (!unlocked.contains(collectionId)) continue;
            resetCollection(playerUuid, collectionId.legacyIndex());
        }
    }

    private void resetCollection(UUID playerUuid, int collectionIndex) {
        List<RuneRecord> initial = players.getRunesInCollection(playerUuid, collectionIndex);
        Set<Integer> processedRuneIndexes = new HashSet<>();
        for (RuneRecord record : initial) {
            if (!processedRuneIndexes.add(record.runeIndex)) continue;
            resetRune(playerUuid, collectionIndex, record.runeIndex, initial.size());
        }
    }

    /**
     * The factual pre-reset rank decides both parts of the selective wipe:
     * ranks below Advanced return to Ordinary and lose enhancement progress; Advanced+ preserves both.
     * Legacy saves may contain more than one physical record for the same runeIndex, so repeat until
     * the factual best rank reaches the policy target. The record-count bound prevents endless loops.
     */
    private void resetRune(UUID playerUuid, int collectionIndex, int runeIndex, int maxAttempts) {
        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            List<RuneRecord> records = players.getRunesInCollection(playerUuid, collectionIndex);
            int bestRank = bestRank(records, runeIndex);
            if (!RunePrestigeResetRules.shouldReset(bestRank)) return;

            int targetRank = RunePrestigeResetRules.rankAfterReset(bestRank);
            if (targetRank != bestRank && !players.setRuneRank(playerUuid, collectionIndex, runeIndex, targetRank)) return;

            enhancements.resetRune(playerUuid, collectionIndex, runeIndex);
            if (targetRank == bestRank) return;
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
