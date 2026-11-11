package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RunePrestigeResetRules;
import java.util.HashSet;
import java.util.List;
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
        // Presence of factual persisted rune records, not potentially stale unlock metadata,
        // determines which collection must be processed. This also covers migrated saves where
        // fixed collections exist but unlockedCollections was not yet populated.
        int owned = 0;
        int eligible = 0;
        int failures = 0;
        for (RuneCollectionId collectionId : RuneCollectionId.values()) {
            int index = collectionId.legacyIndex();
            List<RuneRecord> runes = players.getRunesInCollection(playerUuid, index);
            owned += uniqueRuneCount(runes);
            int[] audit = resetCollection(playerUuid, index, runes);
            eligible += audit[0];
            failures += audit[1];
        }
        RevolutionMod.logger.info("Mastery rune reset audit: owned=" + owned +
                ", eligible=" + eligible + ", verificationFailures=" + failures);
        if (failures != 0) RevolutionMod.logger.error(
                "Mastery rune reset incomplete: " + failures + " persisted rune(s) still violate reset policy");
    }

    /** Returns [eligibleRunes, verificationFailures] without mutating any absent rune. */
    private int[] resetCollection(UUID playerUuid, int collectionIndex, List<RuneRecord> initial) {
        java.util.Set<Integer> processed = new java.util.HashSet<>();
        int eligible = 0;
        int failures = 0;
        for (RuneRecord record : initial) {
            if (!processed.add(record.runeIndex)) continue;
            int originalRank = bestRank(initial, record.runeIndex);
            // ORDINARY may still hold enhancement levels; reset those too.
            if (!RunePrestigeResetRules.shouldReset(originalRank)) continue;
            eligible++;
            resetRune(playerUuid, collectionIndex, record.runeIndex, initial.size());
            int after = bestRank(players.getRunesInCollection(playerUuid, collectionIndex), record.runeIndex);
            int enhancement = enhancements.getLevel(playerUuid, collectionIndex, record.runeIndex);
            if (after != RunePrestigeResetRules.rankAfterReset(originalRank) || enhancement != 0) {
                failures++;
                RevolutionMod.logger.error("Mastery rune reset verification failed: collection=" +
                        collectionIndex + ", rune=" + record.runeIndex + ", beforeRank=" + originalRank +
                        ", afterRank=" + after + ", enhancement=" + enhancement);
            }
        }
        return new int[] { eligible, failures };
    }

    /**
     * The factual pre-reset rank decides both parts of the selective wipe:
     * ranks below Advanced return to Ordinary and lose enhancement progress; Advanced+ preserves both.
     * Legacy saves may contain duplicate physical records. Re-check persisted best rank until
     * the policy target is reached, bounded by the original number of records.
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

    private static int uniqueRuneCount(List<RuneRecord> runes) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (RuneRecord rune : runes) seen.add(rune.runeIndex);
        return seen.size();
    }

    private static int bestRank(List<RuneRecord> records, int runeIndex) {
        int best = 0;
        for (RuneRecord record : records) {
            if (record.runeIndex == runeIndex && record.layer > best) best = record.layer;
        }
        return best;
    }
}
