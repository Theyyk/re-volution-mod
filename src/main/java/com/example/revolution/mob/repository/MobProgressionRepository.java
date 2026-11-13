package com.example.revolution.mob.repository;

import java.util.Set;
import java.util.UUID;

/** Persistence port for per-player mob-zone progression unlocks. */
public interface MobProgressionRepository {
    Set<Integer> getUnlockedSlots(UUID playerUuid, int worldLocationId);
    boolean unlockSlot(UUID playerUuid, int worldLocationId, int mobSlotId);
    boolean replaceUnlockedSlots(UUID playerUuid, int worldLocationId, Set<Integer> unlockedSlots);
    boolean clearFromLocation(UUID playerUuid, int worldLocationId);
    /** Explicitly classify unmarked historical stage grants as run-earned. No destructive delete. */
    default boolean classifyLegacyRunSlots(UUID playerUuid, int worldLocationId) { return false; }
}
