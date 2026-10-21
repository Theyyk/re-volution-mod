package com.example.revolution.mob.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Pure ordering and gating rules for mob-zone progression. */
public final class MobProgressionRules {
    private MobProgressionRules() {}

    public static List<Integer> normalizeConfiguredSlots(Collection<Integer> slots) {
        TreeSet<Integer> normalized = new TreeSet<>();
        if (slots != null) {
            for (Integer slot : slots) if (slot != null && slot >= 1 && slot <= 99) normalized.add(slot);
        }
        return Collections.unmodifiableList(new ArrayList<>(normalized));
    }

    public static boolean isUnlocked(int mobSlotId, Collection<Integer> configuredSlots, Set<Integer> explicitUnlocks) {
        List<Integer> configured = normalizeConfiguredSlots(configuredSlots);
        if (configured.isEmpty() || !configured.contains(mobSlotId)) return false;
        if (mobSlotId == configured.get(0)) return true;
        return explicitUnlocks != null && explicitUnlocks.contains(mobSlotId);
    }

    public static Integer nextConfiguredSlot(int mobSlotId, Collection<Integer> configuredSlots) {
        for (Integer slot : normalizeConfiguredSlots(configuredSlots)) if (slot > mobSlotId) return slot;
        return null;
    }

    public static Integer previousConfiguredSlot(int mobSlotId, Collection<Integer> configuredSlots) {
        Integer previous = null;
        for (Integer slot : normalizeConfiguredSlots(configuredSlots)) {
            if (slot >= mobSlotId) break;
            previous = slot;
        }
        return previous;
    }
}
