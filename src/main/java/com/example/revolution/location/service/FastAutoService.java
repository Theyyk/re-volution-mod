package com.example.revolution.location.service;

import com.example.revolution.location.repository.FastAutoPreferenceRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure player-level fast-auto policy. Unlock events only queue an already-authorized target;
 * Minecraft movement is executed later by the Forge adapter on the server player tick.
 */
public final class FastAutoService implements MobProgressionService.UnlockListener {
    private final FastAutoPreferenceRepository preferences;
    private final MobProgressionService progression;
    private final Map<UUID, Integer> pendingTargets = new ConcurrentHashMap<>();

    public FastAutoService(FastAutoPreferenceRepository preferences, MobProgressionService progression) {
        if (preferences == null) throw new IllegalArgumentException("preferences is required");
        if (progression == null) throw new IllegalArgumentException("progression is required");
        this.preferences = preferences;
        this.progression = progression;
    }

    public boolean isEnabled(UUID playerUuid) {
        return playerUuid != null && preferences.isFastAutoEnabled(playerUuid);
    }

    public void setEnabled(UUID playerUuid, boolean enabled) {
        if (playerUuid == null) return;
        preferences.setFastAutoEnabled(playerUuid, enabled);
        if (!enabled) pendingTargets.remove(playerUuid);
    }

    @Override
    public void onUnlocked(UUID playerUuid, MobProgressionService.UnlockResult result) {
        if (playerUuid == null || result == null || !isEnabled(playerUuid)) return;
        pendingTargets.put(playerUuid, result.internalLocationId());
    }

    /** Returns one still-unlocked pending target and consumes it. */
    public Integer pollPendingTarget(UUID playerUuid) {
        if (playerUuid == null || !isEnabled(playerUuid)) {
            if (playerUuid != null) pendingTargets.remove(playerUuid);
            return null;
        }
        Integer target = pendingTargets.remove(playerUuid);
        if (target == null || !progression.isUnlocked(playerUuid, target)) return null;
        return target;
    }

    public void forgetPlayer(UUID playerUuid) {
        if (playerUuid != null) pendingTargets.remove(playerUuid);
    }

    public void clear() {
        pendingTargets.clear();
    }
}
