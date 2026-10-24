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
    public int unlockWrites;
    public int replaceWrites;
    public int clearWrites;

    public void seed(UUID player, int location, Integer... slots) {
        Set<Integer> values = new HashSet<>();
        Collections.addAll(values, slots);
        records.computeIfAbsent(player, ignored -> new HashMap<>()).put(location, values);
    }

    @Override public Set<Integer> getUnlockedSlots(UUID player, int location) {
        return new HashSet<>(records.getOrDefault(player, Collections.emptyMap())
                .getOrDefault(location, Collections.emptySet()));
    }

    @Override public boolean unlockSlot(UUID player, int location, int slot) {
        unlockWrites++;
        if (failUnlock) return false;
        return records.computeIfAbsent(player, ignored -> new HashMap<>())
                .computeIfAbsent(location, ignored -> new HashSet<>()).add(slot);
    }

    @Override public boolean replaceUnlockedSlots(UUID player, int location, Set<Integer> slots) {
        replaceWrites++;
        if (failReplace) return false;
        records.computeIfAbsent(player, ignored -> new HashMap<>()).put(location, new HashSet<>(slots));
        return true;
    }

    @Override public boolean clearFromLocation(UUID player, int location) {
        clearWrites++;
        if (failClear) return false;
        Map<Integer, Set<Integer>> values = records.get(player);
        if (values != null) values.keySet().removeIf(id -> id >= location);
        return true;
    }
}
