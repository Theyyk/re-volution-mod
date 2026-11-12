package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneProgression;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Post-commit verification and fallback selective ownership removal.
 * The Mongo prestige repository commits the same removal atomically with the run.
 * Only ORDINARY and IMPROVED vanish. ADVANCED+ are preserved with enhancements.
 */
public final class RunePrestigeResetService {
    private static final Logger LOG = LogManager.getLogger(RunePrestigeResetService.class);
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;

    public RunePrestigeResetService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        if (players == null) throw new IllegalArgumentException("player repository is required");
        if (enhancements == null) throw new IllegalArgumentException("enhancement repository is required");
        this.players = players;
        this.enhancements = enhancements;
    }

    public void reset(UUID playerUuid) {
        int removed = 0;
        int remainingLowRank = 0;
        for (RuneCollectionId collection : RuneCollectionId.values()) {
            int index = collection.legacyIndex();
            List<RuneRecord> records = players.getRunesInCollection(playerUuid, index);
            List<RuneRecord> retained = new ArrayList<>();
            Set<Integer> retainedIndices = new HashSet<>();
            Set<Integer> removedIndices = new HashSet<>();
            for (RuneRecord record : records) {
                if (record.layer >= RuneProgression.ADVANCED_RANK) {
                    retained.add(record);
                    retainedIndices.add(record.runeIndex);
                } else {
                    removedIndices.add(record.runeIndex);
                    removed++;
                }
            }
            if (!removedIndices.isEmpty()) {
                // Usually no-op for Mongo: the guarded commit has already removed
                // these records. For alternate repositories, reconstruct only the
                // retained ADVANCED+ records without dropping their enhancements.
                players.clearCollection(playerUuid, index);
                for (RuneRecord rune : retained)
                    players.addRuneToCollection(playerUuid, index,
                            rune.slot, rune.runeIndex, rune.layer, rune.level);
                for (Integer runeIndex : removedIndices)
                    if (!retainedIndices.contains(runeIndex))
                        enhancements.resetRune(playerUuid, index, runeIndex);
            }
            for (RuneRecord rune : players.getRunesInCollection(playerUuid, index))
                if (rune.layer < RuneProgression.ADVANCED_RANK) remainingLowRank++;
        }
        LOG.info("Mastery selective rune ownership reset audit: removed=" + removed +
                ", remainingLowRank=" + remainingLowRank);
        if (remainingLowRank != 0) LOG.error(
                "Mastery selective rune ownership reset incomplete: remainingLowRank=" + remainingLowRank);
    }
}
