package com.example.revolution.mob.repository;

import java.util.Set;
import java.util.UUID;

/** Persistence port for per-player mob-zone progression unlocks. */
public interface MobProgressionRepository {
    Set<Integer> getUnlockedSlots(UUID playerUuid, int worldLocationId);
    boolean unlockSlot(UUID playerUuid, int worldLocationId, int mobSlotId);
    boolean replaceUnlockedSlots(UUID playerUuid, int worldLocationId, Set<Integer> unlockedSlots);
}
