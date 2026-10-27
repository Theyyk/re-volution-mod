package com.example.revolution.mob.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.mob.repository.MobProgressionRepository;
import com.example.revolution.mob.rules.MobProgressionRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-player progression across configured mob stages and world Locations. */
public final class MobProgressionService {
    public interface UnlockListener { void onUnlocked(UUID playerUuid, UnlockResult result); }

    public static final class UnlockResult {
        public final int worldLocationId;
        public final int mobSlotId;
        public final String mobName;
        UnlockResult(int worldLocationId, int mobSlotId, String mobName) {
            this.worldLocationId = worldLocationId;
            this.mobSlotId = mobSlotId;
            this.mobName = mobName;
        }
        public int internalLocationId() { return MobZoneIds.encode(worldLocationId, mobSlotId); }
    }

    private static final class Stage {
        final int internalLocationId;
        final int mobSlotId;
        final String mobName;
        final boolean boss;
        Stage(int internalLocationId, int mobSlotId, String mobName, boolean boss) {
            this.internalLocationId = internalLocationId;
            this.mobSlotId = mobSlotId;
            this.mobName = mobName;
            this.boss = boss;
        }
    }

    private final MobProgressionRepository repository;
    private final Map<Integer, List<Stage>> chains = new LinkedHashMap<>();
    private final Set<Integer> validBossLayouts = new LinkedHashSet<>();
    private final Map<String, Set<Integer>> unlockCache = new HashMap<>();
    private UnlockListener unlockListener;

    public MobProgressionService(MobProgressionRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
    }

    public void setUnlockListener(UnlockListener unlockListener) { this.unlockListener = unlockListener; }

    public void reloadConfiguration(List<LocationDefinition> locations, List<StoredMobSpawn> storedSpawns) {
        chains.clear();
        validBossLayouts.clear();
        unlockCache.clear();
        if (locations == null || storedSpawns == null) return;

        Map<Integer, LocationDefinition> zones = new LinkedHashMap<>();
        for (LocationDefinition location : locations) {
            if (location != null && MobZoneIds.isMobZoneId(location.getLocationId())) {
                zones.put(location.getLocationId(), location);
            }
        }

        // A stage exists because its zone definition exists. Population is optional metadata and must not
        // remove an otherwise configured stage from progression topology (for example a freshly created
        // first stage of the next Location that has not been populated yet).
        Map<Integer, String> populatedZones = new HashMap<>();
        for (StoredMobSpawn spawn : storedSpawns) {
            if (spawn == null || !spawn.isEnabled()) continue;
            int internalLocationId = spawn.getLocationId();
            if (!zones.containsKey(internalLocationId)) continue;
            populatedZones.putIfAbsent(internalLocationId, spawn.getMobName());
        }

        Map<Integer, Map<Integer, Stage>> staged = new LinkedHashMap<>();
        for (LocationDefinition zone : zones.values()) {
            int internalLocationId = zone.getLocationId();
            int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
            int mobSlotId = MobZoneIds.mobSlotId(internalLocationId);
            String mobName = populatedZones.get(internalLocationId);
            if (mobName == null) mobName = "";
            // Boss identity is topology-derived below. Persisted BOSS_ZONE remains accepted as legacy metadata,
            // but it cannot make a non-final stage the runtime boss.
            staged.computeIfAbsent(worldLocationId, ignored -> new LinkedHashMap<>())
                    .put(mobSlotId, new Stage(internalLocationId, mobSlotId, mobName, false));
        }

        List<Integer> worldLocations = new ArrayList<>(staged.keySet());
        Collections.sort(worldLocations);
        for (Integer worldLocationId : worldLocations) {
            Map<Integer, Stage> bySlot = staged.get(worldLocationId);
            List<Integer> sortedSlots = MobProgressionRules.normalizeConfiguredSlots(bySlot.keySet());
            List<Stage> chain = new ArrayList<>();
            for (int i = 0; i < sortedSlots.size(); i++) {
                Stage configured = bySlot.get(sortedSlots.get(i));
                boolean boss = i == sortedSlots.size() - 1;
                chain.add(new Stage(
                        configured.internalLocationId,
                        configured.mobSlotId,
                        configured.mobName,
                        boss));
            }
            List<Stage> immutable = Collections.unmodifiableList(chain);
            chains.put(worldLocationId, immutable);
            if (!chain.isEmpty()) validBossLayouts.add(worldLocationId);
        }
    }

    public boolean isUnlocked(UUID playerUuid, int internalLocationId) {
        if (playerUuid == null || !MobZoneIds.isMobZoneId(internalLocationId)) return true;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        int mobSlotId = MobZoneIds.mobSlotId(internalLocationId);
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(mobSlotId)) return true;
        Set<Integer> explicit = unlockedSlots(playerUuid, worldLocationId);
        Integer firstLocation = firstConfiguredWorldLocationId();
        int firstSlot = configured.get(0);
        boolean firstLocationDefault = firstLocation != null && firstLocation == worldLocationId;
        if (!firstLocationDefault && !explicit.contains(firstSlot)) return false;
        if (mobSlotId == firstSlot) return firstLocationDefault || explicit.contains(firstSlot);
        return explicit.contains(mobSlotId);
    }

    public Integer previousRequiredSlot(int internalLocationId) {
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return null;
        return MobProgressionRules.previousConfiguredSlot(MobZoneIds.mobSlotId(internalLocationId),
                configuredSlots(MobZoneIds.worldLocationId(internalLocationId)));
    }

    public Integer nextConfiguredInternalLocationId(int internalLocationId) {
        return adjacentConfiguredInternalLocationId(internalLocationId, true);
    }

    public Integer previousConfiguredInternalLocationId(int internalLocationId) {
        return adjacentConfiguredInternalLocationId(internalLocationId, false);
    }

    public Integer nextUnlockedInternalLocationId(UUID playerUuid, int internalLocationId) {
        return adjacentUnlockedInternalLocationId(playerUuid, internalLocationId, true);
    }

    public Integer previousUnlockedInternalLocationId(UUID playerUuid, int internalLocationId) {
        return adjacentUnlockedInternalLocationId(playerUuid, internalLocationId, false);
    }

    private Integer adjacentConfiguredInternalLocationId(int internalLocationId, boolean forward) {
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return null;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        int currentSlot = MobZoneIds.mobSlotId(internalLocationId);
        List<Stage> chain = chains.get(worldLocationId);
        if (chain == null || chain.isEmpty()) return null;
        int index = stageIndex(chain, currentSlot);
        if (index < 0) return null;
        if (forward && index + 1 < chain.size()) return chain.get(index + 1).internalLocationId;
        if (!forward && index > 0) return chain.get(index - 1).internalLocationId;

        List<Integer> locations = configuredWorldLocations();
        int locationIndex = locations.indexOf(worldLocationId);
        if (locationIndex < 0) return null;
        int adjacentLocationIndex = forward ? locationIndex + 1 : locationIndex - 1;
        if (adjacentLocationIndex < 0 || adjacentLocationIndex >= locations.size()) return null;

        if (forward) {
            if (!validBossLayouts.contains(worldLocationId)) return null;
            Stage current = chain.get(index);
            if (!current.boss || index != chain.size() - 1) return null;
            List<Stage> nextChain = chains.get(locations.get(adjacentLocationIndex));
            return nextChain == null || nextChain.isEmpty() ? null : nextChain.get(0).internalLocationId;
        }

        int previousWorldLocationId = locations.get(adjacentLocationIndex);
        if (!validBossLayouts.contains(previousWorldLocationId)) return null;
        List<Stage> previousChain = chains.get(previousWorldLocationId);
        if (previousChain == null || previousChain.isEmpty()) return null;
        return previousChain.get(previousChain.size() - 1).internalLocationId;
    }

    private int stageIndex(List<Stage> chain, int slot) {
        for (int i = 0; i < chain.size(); i++) if (chain.get(i).mobSlotId == slot) return i;
        return -1;
    }

    private Integer adjacentUnlockedInternalLocationId(UUID playerUuid, int internalLocationId, boolean forward) {
        if (playerUuid == null) return null;
        Integer adjacentId = adjacentConfiguredInternalLocationId(internalLocationId, forward);
        return adjacentId != null && isUnlocked(playerUuid, adjacentId) ? adjacentId : null;
    }

    public UnlockResult unlockNext(UUID playerUuid, int internalLocationId) {
        if (playerUuid == null || !MobZoneIds.isMobZoneId(internalLocationId) || !isUnlocked(playerUuid, internalLocationId)) return null;
        Integer nextInternalId = nextConfiguredInternalLocationId(internalLocationId);
        if (nextInternalId == null) return null;
        int nextWorldLocationId = MobZoneIds.worldLocationId(nextInternalId);
        int nextSlot = MobZoneIds.mobSlotId(nextInternalId);
        Set<Integer> unlocked = unlockedSlots(playerUuid, nextWorldLocationId);
        if (unlocked.contains(nextSlot)) return null;
        if (!repository.unlockSlot(playerUuid, nextWorldLocationId, nextSlot)) return null;
        unlocked.add(nextSlot);
        Stage next = stage(nextWorldLocationId, nextSlot);
        UnlockResult result = new UnlockResult(nextWorldLocationId, nextSlot, next == null ? "" : next.mobName);
        UnlockListener listener = unlockListener;
        if (listener != null) listener.onUnlocked(playerUuid, result);
        return result;
    }

    /** True for the final configured stage of a Location. */
    public boolean isFinalStage(int internalLocationId) {
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return false;
        int worldLocationId = MobZoneIds.worldLocationId(internalLocationId);
        if (!validBossLayouts.contains(worldLocationId)) return false;
        List<Stage> chain = chains.get(worldLocationId);
        if (chain == null || chain.isEmpty()) return false;
        return chain.get(chain.size() - 1).internalLocationId == internalLocationId;
    }

    public boolean hasValidBossLayout(int worldLocationId) {
        return validBossLayouts.contains(worldLocationId);
    }

    public boolean isBossStage(int internalLocationId) {
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return false;
        Stage stage = stage(MobZoneIds.worldLocationId(internalLocationId), MobZoneIds.mobSlotId(internalLocationId));
        return stage != null && stage.boss;
    }

    public List<Integer> configuredWorldLocations() {
        List<Integer> result = new ArrayList<>(chains.keySet());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public Integer firstConfiguredWorldLocationId() {
        List<Integer> locations = configuredWorldLocations();
        return locations.isEmpty() ? null : locations.get(0);
    }

    public boolean revokeFrom(UUID playerUuid, int worldLocationId, int mobSlotId) {
        if (playerUuid == null || worldLocationId < 0) return false;
        List<Integer> configured = configuredSlots(worldLocationId);
        if (configured.isEmpty() || !configured.contains(mobSlotId)) return false;
        Set<Integer> retained = new LinkedHashSet<>();
        for (Integer slot : unlockedSlots(playerUuid, worldLocationId)) if (slot != null && slot < mobSlotId) retained.add(slot);
        if (!repository.replaceUnlockedSlots(playerUuid, worldLocationId, retained)) return false;
        if (worldLocationId < Integer.MAX_VALUE && !repository.clearFromLocation(playerUuid, worldLocationId + 1)) return false;
        invalidateFromLocation(playerUuid, worldLocationId);
        return true;
    }

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
            } catch (NumberFormatException ignored) { remove.add(key); }
        }
        for (String key : remove) unlockCache.remove(key);
    }

    public void clear() {
        chains.clear();
        validBossLayouts.clear();
        unlockCache.clear();
        unlockListener = null;
    }
}
