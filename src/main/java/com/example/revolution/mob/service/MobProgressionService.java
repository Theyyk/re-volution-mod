package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.mob.repository.MobProgressionRepository;
import com.example.revolution.mob.rules.MobProgressionRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player progression across configured mob-zone slots.
 * The first populated slot in each world Location is open by default; killing a stage as the
 * v1.3.7 max-contributor unlocks the next populated slot by ascending mobSlotId.
 */
public final class MobProgressionService {
    public static final class UnlockResult {
        public final int worldLocationId;
        public final int mobSlotId;
        public final String mobName;

        UnlockResult(int worldLocationId, int mobSlotId, String mobName) {
            this.worldLocationId = worldLocationId;
            this.mobSlotId = mobSlotId;
            this.mobName = mobName;
        }
    }

    private static final class Stage {
        final int internalLocationId;
        final int mobSlotId;
        final String mobName;

        Stage(int internalLocationId, int mobSlotId, String mobName) {
            this.internalLocationId = internalLocationId;
            this.mobSlotId = mobSlotId;
            this.mobName = mobName;
        }
    }

    private final MobProgressionRepository repository;
    private final Map<Integer, List<Stage>> chains = new LinkedHashMap<>();
    private final Map<String, Set<Integer>> unlockCache = new HashMap<>();

    public MobProgressionService(MobProgressionRepository repository) {
        this.repository = repository;
    }

    /** Rebuilds the stage chain from the same Location/Mob Spawn snapshot used by managed runtime. */
    public void reloadConfiguration(List<LocationDefinition> locations, List<StoredMobSpawn> storedSpawns) {
        chains.clear();
        if (locations == null || storedSpawns == null) return;

        Set<Integer> existingZoneIds = new HashSet<>();
        for (LocationDefinition location : locations) {
            if (location != null && MobZoneIds.isMobZoneId(location.getLocationId())) {
                existingZoneIds.add(location.getLocationId());
            }
        }

        Map<Integer, String> populatedZones = new LinkedHashMap<>();
        for (StoredMobSpawn spawn : storedSpawns) {
            if (spawn == null || !spawn.isEnabled()) continue;
            int internalLocationId = spawn.getLocationId();
            if (!existingZoneIds.contains(internalLocationId) || !MobZoneIds.isMobZoneId(internalLocationId)) continue;
            populatedZones.putIfAbsent(internalLocationId, spawn.getMobName());
        }

        Map<Integer, Map<Integer, Stage>> staged = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> item : populatedZones.entrySet()) {
            int internalLocationId = item.getKey();
            int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
            int mobSlotId = MobZoneIds.mobSlotId(internalLocationId);
            staged.computeIfAbsent(worldLocationId, ignored -> new LinkedHashMap<>())
                    .put(mobSlotId, new Stage(internalLocationId, mobSlotId, item.getValue()));
        }

        for (Map.Entry<Integer, Map<Integer, Stage>> item : staged.entrySet()) {
            List<Integer> sortedSlots = MobProgressionRules.normalizeConfiguredSlots(item.getValue().keySet());
            List<Stage> chain = new ArrayList<>();
            for (Integer slot : sortedSlots) chain.add(item.getValue().get(slot));
            chains.put(item.getKey(), Collections.unmodifiableList(chain));
        }
    }

    public boolean isUnlocked(UUID playerUuid, int internalLocationId) {
        if (playerUuid == null || !MobZoneIds.isMobZoneId(internalLocationId)) return true;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        int mobSlotId = MobZoneIds.mobSlotId(internalLocationId);
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(mobSlotId)) return true;
        return MobProgressionRules.isUnlocked(mobSlotId, configured, unlockedSlots(playerUuid, worldLocationId));
    }

    public Integer previousRequiredSlot(int internalLocationId) {
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return null;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        return MobProgressionRules.previousConfiguredSlot(
                MobZoneIds.mobSlotId(internalLocationId), configuredSlots(worldLocationId));
    }

    /**
     * Resolves the next configured stage in the same world Location only when that stage is already
     * unlocked for the player. This is navigation data only; player movement belongs to Location runtime.
     */
    public Integer nextUnlockedInternalLocationId(UUID playerUuid, int internalLocationId) {
        return adjacentUnlockedInternalLocationId(playerUuid, internalLocationId, true);
    }

    /**
     * Resolves the previous configured stage in the same world Location only when it is accessible
     * to the player. This does not execute or schedule a transition.
     */
    public Integer previousUnlockedInternalLocationId(UUID playerUuid, int internalLocationId) {
        return adjacentUnlockedInternalLocationId(playerUuid, internalLocationId, false);
    }

    private Integer adjacentUnlockedInternalLocationId(UUID playerUuid, int internalLocationId, boolean forward) {
        if (playerUuid == null || !MobZoneIds.isMobZoneId(internalLocationId)) return null;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        int currentSlot = MobZoneIds.mobSlotId(internalLocationId);
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(currentSlot)) return null;

        Integer adjacentSlot = forward
                ? MobProgressionRules.nextConfiguredSlot(currentSlot, configured)
                : MobProgressionRules.previousConfiguredSlot(currentSlot, configured);
        if (adjacentSlot == null) return null;
        Stage adjacent = stage(worldLocationId, adjacentSlot);
        if (adjacent == null || !isUnlocked(playerUuid, adjacent.internalLocationId)) return null;
        return adjacent.internalLocationId;
    }

    public UnlockResult unlockNext(UUID playerUuid, int internalLocationId) {
        if (playerUuid == null || !MobZoneIds.isMobZoneId(internalLocationId)) return null;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        int currentSlot = MobZoneIds.mobSlotId(internalLocationId);
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(currentSlot)) return null;
        Integer nextSlot = MobProgressionRules.nextConfiguredSlot(currentSlot, configured);
        if (nextSlot == null) return null;

        Set<Integer> unlocked = unlockedSlots(playerUuid, worldLocationId);
        if (unlocked.contains(nextSlot)) return null;
        if (!repository.unlockSlot(playerUuid, worldLocationId, nextSlot)) return null;
        unlocked.add(nextSlot);
        Stage next = stage(worldLocationId, nextSlot);
        return new UnlockResult(worldLocationId, nextSlot, next == null ? "" : next.mobName);
    }

    /**
     * Revokes access from one stage forward. The requested stage and all later slots in the same
     * world Location are closed, then every later world Location progression record is cleared.
     */
    public boolean revokeFrom(UUID playerUuid, int worldLocationId, int mobSlotId) {
        if (playerUuid == null || worldLocationId < 0) return false;
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(mobSlotId)) return false;

        Set<Integer> retained = new LinkedHashSet<>();
        for (Integer slot : unlockedSlots(playerUuid, worldLocationId)) {
            if (slot != null && slot < mobSlotId) retained.add(slot);
        }
        if (!repository.replaceUnlockedSlots(playerUuid, worldLocationId, retained)) return false;
        if (worldLocationId < Integer.MAX_VALUE
                && !repository.clearFromLocation(playerUuid, worldLocationId + 1)) return false;
        invalidateFromLocation(playerUuid, worldLocationId);
        return true;
    }

    /** Clears this world Location and every later world Location back to default progression. */
    public boolean resetFromLocation(UUID playerUuid, int worldLocationId) {
        if (playerUuid == null || worldLocationId < 0) return false;
        if (!repository.clearFromLocation(playerUuid, worldLocationId)) return false;
        invalidateFromLocation(playerUuid, worldLocationId);
        return true;
    }

    public List<Integer> configuredSlots(int worldLocationId) {
        List<Stage> chain = chains.get(worldLocationId);
        if (chain == null || chain.isEmpty()) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (Stage stage : chain) result.add(stage.mobSlotId);
        return Collections.unmodifiableList(result);
    }

    private Stage stage(int worldLocationId, int mobSlotId) {
        List<Stage> chain = chains.get(worldLocationId);
        if (chain == null) return null;
        for (Stage stage : chain) if (stage.mobSlotId == mobSlotId) return stage;
        return null;
    }

    private Set<Integer> unlockedSlots(UUID playerUuid, int worldLocationId) {
        String key = playerUuid.toString() + ":" + worldLocationId;
        Set<Integer> cached = unlockCache.get(key);
        if (cached != null) return cached;
        Set<Integer> loaded = new LinkedHashSet<>(repository.getUnlockedSlots(playerUuid, worldLocationId));
        unlockCache.put(key, loaded);
        return loaded;
    }

    private void invalidateFromLocation(UUID playerUuid, int worldLocationId) {
        String prefix = playerUuid.toString() + ":";
        List<String> remove = new ArrayList<>();
        for (String key : unlockCache.keySet()) {
            if (!key.startsWith(prefix)) continue;
            try {
                int cachedLocationId = Integer.parseInt(key.substring(prefix.length()));
                if (cachedLocationId >= worldLocationId) remove.add(key);
            } catch (NumberFormatException ignored) {
                remove.add(key);
            }
        }
        for (String key : remove) unlockCache.remove(key);
    }

    public void clear() {
        chains.clear();
        unlockCache.clear();
    }
}
