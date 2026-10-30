package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerIdentity;
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RunePrestigeResetServiceTest {
    @Test
    void selectiveWipeUsesPreResetRankForRankAndEnhancementPolicy() {
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

        assertEquals(RuneProgression.ORDINARY_RANK, players.rank(0));
        assertEquals(RuneProgression.ORDINARY_RANK, players.rank(1));
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
    void missingRuneDoesNotCreateOrWipeEnhancement() {
        UUID playerUuid = UUID.randomUUID();
        FakePlayerRepository players = new FakePlayerRepository();
        FakeEnhancementRepository enhancements = new FakeEnhancementRepository();
        enhancements.levels.put(5, 25);

        new RunePrestigeResetService(players, enhancements).reset(playerUuid);

        assertEquals(25, enhancements.getLevel(playerUuid, 0, 5));
    }

    private static final class FakePlayerRepository implements PlayerRepository {
        private final List<RuneRecord> runes = new ArrayList<>();

        void addRune(int runeIndex, int rank) {
            runes.add(new RuneRecord(runeIndex, runeIndex, rank, 0));
        }

        int rank(int runeIndex) {
            for (RuneRecord rune : runes) if (rune.runeIndex == runeIndex) return rune.layer;
            return 0;
        }

        @Override public List<PlayerIdentity> listKnownPlayers() { return Collections.emptyList(); }
        @Override public void rememberIdentity(UUID uuid, String lastKnownName) {}
        @Override public int getBalance(UUID uuid) { return 0; }
        @Override public void setBalance(UUID uuid, int balance) {}
        @Override public int getCrystals(UUID uuid) { return 0; }
        @Override public void setCrystals(UUID uuid, int crystals) {}
        @Override public List<RuneRecord> getRunesInCollection(UUID uuid, int collectionIndex) {
            return collectionIndex == 0 ? new ArrayList<>(runes) : Collections.emptyList();
        }
        @Override public int getActiveCollection(UUID uuid) { return 0; }
        @Override public void setActiveCollection(UUID uuid, int collectionIndex) {}
        @Override public List<String> getCollectionNames(UUID uuid) { return Arrays.asList("Default"); }
        @Override public void addCollection(UUID uuid, String name) {}
        @Override public void deleteCollection(UUID uuid, int collectionIndex) {}
        @Override public void addRuneToCollection(UUID uuid, int collectionIndex, int slot, int runeIndex, int layer, int level) {}
        @Override public boolean setRuneRank(UUID uuid, int collectionIndex, int runeIndex, int layer) {
            for (int i = 0; i < runes.size(); i++) {
                RuneRecord rune = runes.get(i);
                if (rune.runeIndex != runeIndex) continue;
                runes.set(i, new RuneRecord(rune.slot, rune.runeIndex, layer, rune.level));
                return true;
            }
            return false;
        }
        @Override public void clearCollection(UUID uuid, int collectionIndex) { runes.clear(); }
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
