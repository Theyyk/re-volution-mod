package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCollectionUnlockRules;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Evaluates the fixed collection chain exclusively from factual per-rune rank state. */
public final class RuneCollectionUnlockService {
    private final PlayerRepository players;
    private final RuneCollectionStateRepository states;
    private final int threshold;

    public RuneCollectionUnlockService(PlayerRepository players, RuneCollectionStateRepository states) {
        this(players, states, RuneCollectionUnlockRules.DEFAULT_THRESHOLD);
    }

    public RuneCollectionUnlockService(
            PlayerRepository players,
            RuneCollectionStateRepository states,
            int threshold) {
        if (players == null || states == null) throw new IllegalArgumentException("rune collection repositories are required");
        if (threshold <= 0 || threshold > 26) throw new IllegalArgumentException("unlock threshold must be 1..26");
        this.players = players;
        this.states = states;
        this.threshold = threshold;
    }

    public Set<RuneCollectionId> unlocked(UUID uuid) {
        return states.getUnlockedCollections(uuid);
    }

    /**
     * Evaluates only the next collection in the chain. Repeated calls are safe and unlocks never
     * regress. This also serves as deterministic legacy migration evaluation for Collection 1.
     */
    public RuneCollectionId evaluateNextUnlock(UUID uuid) {
        Set<RuneCollectionId> unlocked = states.getUnlockedCollections(uuid);
        RuneCollectionId source = highestContiguousUnlocked(unlocked);
        if (source == RuneCollectionId.COLLECTION_07) return null;

        RuneCollectionId target = RuneCollectionId.fromLegacyIndex(source.legacyIndex() + 1);
        if (target == null || unlocked.contains(target)) return null;
        if (!RuneCollectionUnlockRules.qualifies(bestRanks(uuid, source), threshold)) return null;
        return states.unlockCollection(uuid, target) ? target : null;
    }

    public int improvedOrHigherProgress(UUID uuid, RuneCollectionId collectionId) {
        if (collectionId == null) return 0;
        return RuneCollectionUnlockRules.countImprovedOrHigher(bestRanks(uuid, collectionId));
    }

    private int[] bestRanks(UUID uuid, RuneCollectionId collectionId) {
        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> records = players.getRunesInCollection(uuid, collectionId.legacyIndex());
        for (RuneRecord record : records) {
            inventory.set(record.slot, record.runeIndex, record.layer);
        }
        return inventory.bestRanks();
    }

    private static RuneCollectionId highestContiguousUnlocked(Set<RuneCollectionId> unlocked) {
        RuneCollectionId highest = RuneCollectionId.COLLECTION_01;
        for (RuneCollectionId candidate : RuneCollectionId.values()) {
            if (!unlocked.contains(candidate)) break;
            highest = candidate;
        }
        return highest;
    }
}
