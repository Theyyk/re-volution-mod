package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneRecord;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCollectionUnlockServiceTest {
    @Test
    void legacyCollectionOneUnlocksCollectionTwoOnceAndNeverRelocks() {
        UUID player = UUID.randomUUID();
        List<RuneRecord> firstCollection = new ArrayList<>();
        for (int rune = 0; rune < 13; rune++) {
            firstCollection.add(new RuneRecord(rune, rune, 2, 1));
        }
        List<String> collectionNames = new ArrayList<>(Collections.singletonList("Legacy"));
        Set<RuneCollectionId> unlocked = EnumSet.of(RuneCollectionId.COLLECTION_01);

        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRunesInCollection":
                            int collection = (int) args[1];
                            return collection == 0 ? new ArrayList<>(firstCollection) : Collections.emptyList();
                        case "getCollectionNames":
                            return new ArrayList<>(collectionNames);
                        case "addCollection":
                            collectionNames.add((String) args[1]);
                            return true;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
        RuneCollectionStateRepository states = new RuneCollectionStateRepository() {
            @Override public Set<RuneCollectionId> getUnlockedCollections(UUID uuid) {
                return EnumSet.copyOf(unlocked);
            }

            @Override public boolean unlockCollection(UUID uuid, RuneCollectionId collectionId) {
                return unlocked.add(collectionId);
            }
        };

        RuneCollectionUnlockService service = new RuneCollectionUnlockService(players, states);
        assertEquals(13, service.improvedOrHigherProgress(player, RuneCollectionId.COLLECTION_01));
        assertEquals(RuneCollectionId.COLLECTION_02, service.evaluateNextUnlock(player));
        assertTrue(service.isUnlocked(player, RuneCollectionId.COLLECTION_02));
        assertEquals(2, collectionNames.size(), "compatibility storage is provisioned exactly once");

        firstCollection.clear();
        firstCollection.add(new RuneRecord(0, 0, 1, 1));
        assertEquals(0, service.improvedOrHigherProgress(player, RuneCollectionId.COLLECTION_01));
        assertTrue(service.isUnlocked(player, RuneCollectionId.COLLECTION_02),
                "unlock remains permanent after factual ranks fall below threshold");
        assertNull(service.evaluateNextUnlock(player), "C3 stays locked because C2 has no qualifying runes");
        assertEquals(2, collectionNames.size(), "re-evaluation does not duplicate compatibility storage");
    }

    @Test
    void qualifyingCollectionsUnlockTheWholeFixedChainThroughCollectionSeven() {
        UUID player = UUID.randomUUID();
        List<List<RuneRecord>> collections = new ArrayList<>();
        for (int collection = 0; collection < RuneCollectionId.values().length; collection++) {
            List<RuneRecord> records = new ArrayList<>();
            for (int rune = 0; rune < 13; rune++) {
                records.add(new RuneRecord(rune, rune, 2, 1));
            }
            collections.add(records);
        }

        List<String> collectionNames = new ArrayList<>(Collections.singletonList("Legacy"));
        Set<RuneCollectionId> unlocked = EnumSet.of(RuneCollectionId.COLLECTION_01);
        PlayerRepository players = (PlayerRepository) Proxy.newProxyInstance(
                PlayerRepository.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRunesInCollection":
                            int collection = (int) args[1];
                            return collection >= 0 && collection < collections.size()
                                    ? new ArrayList<>(collections.get(collection))
                                    : Collections.emptyList();
                        case "getCollectionNames":
                            return new ArrayList<>(collectionNames);
                        case "addCollection":
                            collectionNames.add((String) args[1]);
                            return true;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
        RuneCollectionStateRepository states = new RuneCollectionStateRepository() {
            @Override public Set<RuneCollectionId> getUnlockedCollections(UUID uuid) {
                return EnumSet.copyOf(unlocked);
            }

            @Override public boolean unlockCollection(UUID uuid, RuneCollectionId collectionId) {
                return unlocked.add(collectionId);
            }
        };

        RuneCollectionUnlockService service = new RuneCollectionUnlockService(players, states);
        RuneCollectionId[] ids = RuneCollectionId.values();
        for (int target = 1; target < ids.length; target++) {
            assertEquals(ids[target], service.evaluateNextUnlock(player),
                    "the next fixed collection unlocks from factual Improved+ progress");
            assertTrue(service.isUnlocked(player, ids[target]));
            assertEquals(target + 1, collectionNames.size(),
                    "legacy backing storage is provisioned exactly through the unlocked collection");
        }

        assertEquals(EnumSet.allOf(RuneCollectionId.class), service.unlocked(player));
        assertNull(service.evaluateNextUnlock(player), "Collection 7 is the terminal collection");
        assertEquals(7, collectionNames.size(), "no eighth compatibility collection is created");
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        if (type == double.class) return 0.0D;
        if (type == char.class) return '\0';
        return null;
    }
}
