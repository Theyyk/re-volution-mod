package com.example.revolution.mob.repository;

import java.util.Set;
import java.util.UUID;

/** Persistence port for per-player mob-zone progression unlocks. */
public interface MobProgressionRepository {
    Set<Integer> getUnlockedSlots(UUID playerUuid, int worldLocationId);
    boolean unlockSlot(UUID playerUuid, int worldLocationId, int mobSlotId);
    /** Additive admin grant. Must never rewrite historical records or remove a legacy run mask. */
    default boolean grantPermanentSlots(UUID playerUuid, int worldLocationId, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) return false;
        Set<Integer> current = getUnlockedSlots(playerUuid, worldLocationId);
        for (Integer slot : slots) {
            if (slot == null || slot < 1 || slot > 99) return false;
            if (!current.contains(slot) && !unlockSlot(playerUuid, worldLocationId, slot)) return false;
        }
        return true;
    }
    boolean replaceUnlockedSlots(UUID playerUuid, int worldLocationId, Set<Integer> unlockedSlots);
    boolean clearFromLocation(UUID playerUuid, int worldLocationId);
    /** Read-only storage provenance, not an authority to revoke any permanent grant. */
    default String stageGrantProvenance(UUID playerUuid, int worldLocationId, int mobSlotId) {
        return "UNSUPPORTED";
    }

    /** Explicitly classify unmarked historical stage grants as run-earned. No destructive delete. */
    default boolean classifyLegacyRunSlots(UUID playerUuid, int worldLocationId) { return false; }
}
