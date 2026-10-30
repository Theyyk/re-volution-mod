package com.example.revolution.rune.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One persistent rune slot and its data-driven effects. */
public final class RuneDefinition {
    public final RuneCollectionId collectionId;
    public final RuneSlotId slotId;
    public final String internalName;
    public final String displayName;
    public final List<RuneEffectDefinition> effects;

    public RuneDefinition(
            RuneCollectionId collectionId,
            RuneSlotId slotId,
            String internalName,
            String displayName,
            List<RuneEffectDefinition> effects) {
        if (collectionId == null) throw new IllegalArgumentException("collection id is required");
        if (slotId == null) throw new IllegalArgumentException("slot id is required");
        this.collectionId = collectionId;
        this.slotId = slotId;
        this.internalName = required(internalName, "internal rune name");
        this.displayName = required(displayName, "display rune name");
        if (effects == null || effects.isEmpty()) throw new IllegalArgumentException("rune must define at least one effect");
        List<RuneEffectDefinition> copy = new ArrayList<>(effects.size());
        for (RuneEffectDefinition effect : effects) {
            if (effect == null) throw new IllegalArgumentException("rune effect cannot be null");
            copy.add(effect);
        }
        this.effects = Collections.unmodifiableList(copy);
    }

    public String identity() {
        return collectionId.id() + "/" + slotId.id();
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }
}
