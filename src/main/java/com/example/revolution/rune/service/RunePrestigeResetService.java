package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Post-commit fallback/verification for Mastery Reset ownership removal.
 * The Mongo prestige repository performs the same wipe atomically with the runId guard.
 * This participant also covers non-Mongo repositories and cleans stale records.
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
        int ownedBefore = 0;
        int remaining = 0;
        int enhancedRemaining = 0;
        for (RuneCollectionId collection : RuneCollectionId.values()) {
            int index = collection.legacyIndex();
            List<RuneRecord> records = players.getRunesInCollection(playerUuid, index);
            ownedBefore += records.size();

            // No ownership of any rarity survives an ordinary run reset.
            if (!records.isEmpty()) players.clearCollection(playerUuid, index);
            enhancements.resetCollection(playerUuid, index);

            List<RuneRecord> persisted = players.getRunesInCollection(playerUuid, index);
            remaining += persisted.size();
            int[] levels = enhancements.getLevels(playerUuid, index);
            for (int level : levels) if (level > 0) enhancedRemaining++;
        }
        LOG.info("Mastery rune ownership reset audit: ownedBefore=" + ownedBefore +
                ", remaining=" + remaining + ", enhancedRemaining=" + enhancedRemaining);
        if (remaining != 0 || enhancedRemaining != 0) {
            LOG.error("Mastery rune ownership reset incomplete: remaining=" + remaining +
                    ", enhancedRemaining=" + enhancedRemaining);
        }
    }
}
