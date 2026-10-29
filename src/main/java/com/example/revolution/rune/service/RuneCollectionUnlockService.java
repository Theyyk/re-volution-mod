package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneLayoutDefinition;
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
        if (threshold <= 0 || threshold > RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            throw new IllegalArgumentException("unlock threshold must be 1.." + RuneLayoutDefinition.SLOTS_PER_COLLECTION);
        }
        this.players = players;
        this.states = states;
        this.threshold = threshold;
    }

    public Set<RuneCollectionId> unlocked(UUID uuid) {
        return states.getUnlockedCollections(uuid);
    }

    public boolean isUnlocked(UUID uuid, RuneCollectionId collectionId) {
        return collectionId != null && states.getUnlockedCollections(uuid).contains(collectionId);
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
        if (!states.unlockCollection(uuid, target)) return null;
        ensureLegacyStorage(uuid, target);
        return target;
    }

    public int improvedOrHigherProgress(UUID uuid, RuneCollectionId collectionId) {
        if (collectionId == null) return 0;
        return RuneCollectionUnlockRules.countImprovedOrHigher(bestRanks(uuid, collectionId));
    }

    public int threshold() {
        return threshold;
    }

    private int[] bestRanks(UUID uuid, RuneCollectionId collectionId) {
        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> records = players.getRunesInCollection(uuid, collectionId.legacyIndex());
        for (RuneRecord record : records) {
            inventory.set(record.slot, record.runeIndex, record.layer);
        }
        return inventory.bestRanks();
    }

    /**
     * The current persistence contract stores collection data in legacy deck indexes. New fixed
     * collections therefore provision missing array entries lazily, while existing entries are
     * preserved byte-for-byte and simply become accessible through the permanent unlock gate.
     */
    private void ensureLegacyStorage(UUID uuid, RuneCollectionId target) {
        int count = players.getCollectionNames(uuid).size();
        while (count <= target.legacyIndex()) {
            int ordinal = count + 1;
            String suffix = ordinal < 10 ? "0" + ordinal : String.valueOf(ordinal);
            players.addCollection(uuid, "Collection " + suffix);
            count++;
        }
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
