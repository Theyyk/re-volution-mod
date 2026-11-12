package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerIdentity;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeBranchRegistry;
import com.example.revolution.prestige.PrestigeRepository;
import com.example.revolution.prestige.PrestigeResetService;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneProgression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RunePrestigeResetServiceTest {
    @Test
    void ordinaryResetDeletesOnlyOrdinaryImprovedAndPreservesAdvancedPlus() {
        UUID playerUuid = UUID.randomUUID();
        FakePlayerRepository players = new FakePlayerRepository();
        FakeEnhancementRepository enhancements = new FakeEnhancementRepository();

        players.addRune(0, RuneProgression.ORDINARY_RANK);
        players.addRune(1, RuneProgression.IMPROVED_RANK);
        players.addRune(2, RuneProgression.ADVANCED_RANK);
        players.addRune(3, RuneProgression.MYTHIC_RANK);
        players.addRune(4, RuneProgression.MYTHIC_RANK + 100);
        for (int runeIndex = 0; runeIndex <= 4; runeIndex++) enhancements.levels.put(runeIndex, 10 + runeIndex);

        new RunePrestigeResetService(players, enhancements).reset(playerUuid);

        assertEquals(3, players.getRunesInCollection(playerUuid, 0).size());
        assertEquals(RuneProgression.ADVANCED_RANK, players.rank(2));
        assertEquals(RuneProgression.MYTHIC_RANK, players.rank(3));
        assertEquals(RuneProgression.MYTHIC_RANK + 100, players.rank(4));

        assertEquals(0, enhancements.getLevel(playerUuid, 0, 0));
        assertEquals(0, enhancements.getLevel(playerUuid, 0, 1));
        assertEquals(12, enhancements.getLevel(playerUuid, 0, 2));
        assertEquals(13, enhancements.getLevel(playerUuid, 0, 3));
        assertEquals(14, enhancements.getLevel(playerUuid, 0, 4));
    }

    @Test
    void resetProcessesPersistedFixedCollectionsEvenWithoutUnlockMetadata() {
        UUID playerId = UUID.randomUUID();
        FakePlayerRepository players = new FakePlayerRepository();
        FakeEnhancementRepository enhancements = new FakeEnhancementRepository();
        // This simulates a migrated save with factual fixed collections 2/3 and no
        // reliable unlockedCollections metadata. Such records must not be skipped.
        players.addRuneInCollection(1, 5, RuneProgression.IMPROVED_RANK);
        players.addRuneInCollection(2, 6, RuneProgression.ORDINARY_RANK);
        players.addRuneInCollection(2, 7, RuneProgression.ADVANCED_RANK);
        enhancements.levels.put(5, 25);
        enhancements.levels.put(6, 13);
        enhancements.levels.put(7, 44);

        new RunePrestigeResetService(players, enhancements).reset(playerId);

        assertEquals(0, players.getRunesInCollection(playerId, 1).size());
        assertEquals(1, players.getRunesInCollection(playerId, 2).size());
        assertEquals(RuneProgression.ADVANCED_RANK, players.rank(2, 7));
        assertEquals(0, enhancements.getLevel(playerId, 1, 5));
        assertEquals(0, enhancements.getLevel(playerId, 2, 6));
        assertEquals(44, enhancements.getLevel(playerId, 2, 7));
    }

    @Test
    void resetDoesNotAlterOrphanedEnhancementsWithoutOwnedRunes() {
        UUID playerUuid = UUID.randomUUID();
        FakePlayerRepository players = new FakePlayerRepository();
        FakeEnhancementRepository enhancements = new FakeEnhancementRepository();
        enhancements.levels.put(5, 25);

        new RunePrestigeResetService(players, enhancements).reset(playerUuid);

        assertEquals(25, enhancements.getLevel(playerUuid, 0, 5));
        assertEquals(0, players.getRunesInCollection(playerUuid, 0).size());
    }

    @Test
    void successfulPrestigeResetInvokesRuneWipeExactlyOnceButDuplicateDoesNot() {
        UUID playerId = UUID.randomUUID();
        FakePlayerRepository players = new FakePlayerRepository();
        FakeEnhancementRepository enhancements = new FakeEnhancementRepository();
        players.addRune(0, RuneProgression.IMPROVED_RANK);
        players.addRune(1, RuneProgression.ADVANCED_RANK);
        enhancements.levels.put(0, 9);
        enhancements.levels.put(1, 11);

        RunePrestigeResetService wipe = new RunePrestigeResetService(players, enhancements);
        AtomicInteger applied = new AtomicInteger();
        MemoryPrestigeRepository repository = new MemoryPrestigeRepository();
        PrestigeBranchRegistry branches = new PrestigeBranchRegistry();
        PrestigeService service = new PrestigeService(repository, branches,
                (uuid, branch) -> branches.get(branch).defaultSaveMultiplier,
                uuid -> { applied.incrementAndGet(); wipe.reset(uuid); });
        String runId = service.startRun(playerId, PrestigeBranch.CLICK).runId;
        assertEquals(PrestigeResetService.ResetResult.APPLIED, service.reset(playerId, runId));
        assertEquals(1, applied.get());
        assertEquals(1, players.getRunesInCollection(playerId, 0).size());
        assertEquals(RuneProgression.ADVANCED_RANK, players.rank(1));
        assertEquals(0, enhancements.getLevel(playerId, 0, 0));
        assertEquals(11, enhancements.getLevel(playerId, 0, 1));
        assertEquals(PrestigeResetService.ResetResult.DUPLICATE, service.reset(playerId, runId));
        assertEquals(1, applied.get());
    }

    private static final class MemoryPrestigeRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> stored = new HashMap<>();
        @Override public PrestigeState load(UUID uuid) {
            PrestigeState state = stored.get(uuid);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID uuid, PrestigeState next) { stored.put(uuid, next.copy()); }
        @Override public ResetCommitResult commitReset(UUID uuid, String expectedRunId, PrestigeState next) {
            PrestigeState old = load(uuid);
            if (old.activeRun == null)
                return expectedRunId.equals(old.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            if (!expectedRunId.equals(old.activeRun.runId)) return ResetCommitResult.STALE;
            save(uuid, next);
            return ResetCommitResult.APPLIED;
        }
    }

    private static final class FakePlayerRepository implements PlayerRepository {
        private final Map<Integer, List<RuneRecord>> byCollection = new HashMap<>();

        void addRune(int runeIndex, int rank) {
            addRuneInCollection(0, runeIndex, rank);
        }

        void addRuneInCollection(int collectionIndex, int runeIndex, int rank) {
            byCollection.computeIfAbsent(collectionIndex, ignored -> new ArrayList<>())
                    .add(new RuneRecord(runeIndex, runeIndex, rank, 0));
        }

        int rank(int runeIndex) { return rank(0, runeIndex); }

        int rank(int collectionIndex, int runeIndex) {
            for (RuneRecord rune : getRunesInCollection(null, collectionIndex))
                if (rune.runeIndex == runeIndex) return rune.layer;
            return 0;
        }

        @Override public List<PlayerIdentity> listKnownPlayers() { return Collections.emptyList(); }
        @Override public void rememberIdentity(UUID uuid, String lastKnownName) {}
        @Override public int getBalance(UUID uuid) { return 0; }
        @Override public void setBalance(UUID uuid, int balance) {}
        @Override public int getCrystals(UUID uuid) { return 0; }
        @Override public void setCrystals(UUID uuid, int crystals) {}
        @Override public List<RuneRecord> getRunesInCollection(UUID uuid, int collectionIndex) {
            List<RuneRecord> runes = byCollection.get(collectionIndex);
            return runes == null ? Collections.emptyList() : new ArrayList<>(runes);
        }
        @Override public int getActiveCollection(UUID uuid) { return 0; }
        @Override public void setActiveCollection(UUID uuid, int collectionIndex) {}
        @Override public List<String> getCollectionNames(UUID uuid) { return Arrays.asList("Default"); }
        @Override public void addCollection(UUID uuid, String name) {}
        @Override public void deleteCollection(UUID uuid, int collectionIndex) {}
        @Override public void addRuneToCollection(UUID uuid, int collectionIndex, int slot, int runeIndex, int layer, int level) {
            byCollection.computeIfAbsent(collectionIndex, ignored -> new ArrayList<>())
                    .add(new RuneRecord(slot, runeIndex, layer, level));
        }
        @Override public boolean setRuneRank(UUID uuid, int collectionIndex, int runeIndex, int layer) {
            List<RuneRecord> runes = byCollection.get(collectionIndex);
            if (runes == null) return false;
            for (int i = 0; i < runes.size(); i++) {
                RuneRecord rune = runes.get(i);
                if (rune.runeIndex != runeIndex) continue;
                runes.set(i, new RuneRecord(rune.slot, rune.runeIndex, layer, rune.level));
                return true;
            }
            return false;
        }
        @Override public void clearCollection(UUID uuid, int collectionIndex) { byCollection.remove(collectionIndex); }
    }

    private static final class FakeEnhancementRepository implements RuneEnhancementRepository {
        private final Map<Integer, Integer> levels = new HashMap<>();

        @Override public int getLevel(UUID uuid, int collectionIndex, int runeIndex) {
            Integer value = levels.get(runeIndex);
            return value == null ? 0 : value;
        }
        @Override public int[] getLevels(UUID uuid, int collectionIndex) { return new int[0]; }
        @Override public int addLevels(UUID uuid, int collectionIndex, int runeIndex, int amount) {
            int next = getLevel(uuid, collectionIndex, runeIndex) + amount;
            levels.put(runeIndex, next);
            return next;
        }
        @Override public void resetRune(UUID uuid, int collectionIndex, int runeIndex) { levels.remove(runeIndex); }
        @Override public void resetCollection(UUID uuid, int collectionIndex) { levels.clear(); }
    }
}
