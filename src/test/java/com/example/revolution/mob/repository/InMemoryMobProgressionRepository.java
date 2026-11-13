package com.example.revolution.mob.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Test fixture: detached reads and explicit write failures, without MongoDB. */
public final class InMemoryMobProgressionRepository implements MobProgressionRepository {
    private final Map<UUID, Map<Integer, Set<Integer>>> records = new HashMap<>();
    public boolean failUnlock;
    public boolean failReplace;
    public boolean failClear;
    public boolean failClassification;
    private final Map<UUID, Map<Integer, Set<Integer>>> maskedLegacy = new HashMap<>();
    private final Map<UUID, Map<Integer, Set<Integer>>> newPermanent = new HashMap<>();
    public int unlockWrites;
    public int replaceWrites;
    public int clearWrites;

    public void seed(UUID player, int location, Integer... slots) {
        Set<Integer> values = new HashSet<>();
        Collections.addAll(values, slots);
        records.computeIfAbsent(player, ignored -> new HashMap<>()).put(location, values);
    }

    public boolean rawLegacyContains(UUID player, int location, int slot) {
        return records.getOrDefault(player, Collections.emptyMap())
                .getOrDefault(location, Collections.emptySet()).contains(slot);
    }

    @Override public Set<Integer> getUnlockedSlots(UUID player, int location) {
        Set<Integer> result = new HashSet<>(records.getOrDefault(player, Collections.emptyMap())
                .getOrDefault(location, Collections.emptySet()));
        result.removeAll(maskedLegacy.getOrDefault(player, Collections.emptyMap())
                .getOrDefault(location, Collections.emptySet()));
        result.addAll(newPermanent.getOrDefault(player, Collections.emptyMap())
                .getOrDefault(location, Collections.emptySet()));
        return result;
    }

    @Override public boolean unlockSlot(UUID player, int location, int slot) {
        unlockWrites++;
        if (failUnlock) return false;
        return newPermanent.computeIfAbsent(player, ignored -> new HashMap<>())
                .computeIfAbsent(location, ignored -> new HashSet<>()).add(slot);
    }

    @Override public boolean replaceUnlockedSlots(UUID player, int location, Set<Integer> slots) {
        replaceWrites++;
        if (failReplace) return false;
        records.computeIfAbsent(player, ignored -> new HashMap<>()).put(location, new HashSet<>(slots));
        maskedLegacy.computeIfAbsent(player, ignored -> new HashMap<>()).remove(location);
        newPermanent.computeIfAbsent(player, ignored -> new HashMap<>()).remove(location);
        return true;
    }

    @Override public boolean classifyLegacyRunSlots(UUID player, int location) {
        if (failClassification) return false;
        maskedLegacy.computeIfAbsent(player, ignored -> new HashMap<>())
                .computeIfAbsent(location, ignored ->
                        new HashSet<>(records.getOrDefault(player, Collections.emptyMap())
                                .getOrDefault(location, Collections.emptySet())));
        return true;
    }

    @Override public boolean clearFromLocation(UUID player, int location) {
        clearWrites++;
        if (failClear) return false;
        Map<Integer, Set<Integer>> values = records.get(player);
        if (values != null) values.keySet().removeIf(id -> id >= location);
        maskedLegacy.computeIfAbsent(player, ignored -> new HashMap<>()).keySet().removeIf(id -> id >= location);
        newPermanent.computeIfAbsent(player, ignored -> new HashMap<>()).keySet().removeIf(id -> id >= location);
        return true;
    }
}
