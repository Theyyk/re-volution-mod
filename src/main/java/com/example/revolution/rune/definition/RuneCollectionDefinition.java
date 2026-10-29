package com.example.revolution.rune.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validated production definition for one 26-slot rune collection. */
public final class RuneCollectionDefinition {
    public final RuneCollectionId id;
    public final String displayName;
    public final List<RuneDefinition> runes;

    public RuneCollectionDefinition(RuneCollectionId id, String displayName, List<RuneDefinition> runes) {
        if (id == null) throw new IllegalArgumentException("collection id is required");
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("collection display name is required");
        }
        if (runes == null || runes.size() != RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            throw new IllegalArgumentException("collection must contain exactly "
                    + RuneLayoutDefinition.SLOTS_PER_COLLECTION + " rune slots");
        }

        Set<RuneSlotId> uniqueSlots = new HashSet<>();
        List<RuneDefinition> copy = new ArrayList<>(runes.size());
        for (RuneDefinition rune : runes) {
            if (rune == null) throw new IllegalArgumentException("rune definition cannot be null");
            if (rune.collectionId != id) {
                throw new IllegalArgumentException("rune belongs to a different collection: " + rune.identity());
            }
            if (!uniqueSlots.add(rune.slotId)) {
                throw new IllegalArgumentException("duplicate rune slot: " + rune.slotId.id());
            }
            copy.add(rune);
        }

        for (int ordinal = 1; ordinal <= RuneLayoutDefinition.SLOTS_PER_COLLECTION; ordinal++) {
            if (!uniqueSlots.contains(RuneSlotId.ofOrdinal(ordinal))) {
                throw new IllegalArgumentException("missing rune slot: " + RuneSlotId.ofOrdinal(ordinal).id());
            }
        }

        this.id = id;
        this.displayName = displayName.trim();
        this.runes = Collections.unmodifiableList(copy);
    }

    public RuneDefinition rune(RuneSlotId slotId) {
        if (slotId == null) return null;
        for (RuneDefinition rune : runes) {
            if (rune.slotId.equals(slotId)) return rune;
        }
        return null;
    }
}
