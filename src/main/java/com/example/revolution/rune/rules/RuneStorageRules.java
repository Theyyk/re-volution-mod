package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;
import java.util.ArrayList;
import java.util.List;

/** Storage-facing boundary for the existing rune rank and legacy-slot rules. */
public final class RuneStorageRules {
    public static final int NORMAL_RANK = RuneLayoutDefinition.NORMAL_RANK;
    public static final int ADVANCED_RANK = RuneLayoutDefinition.ADVANCED_RANK;

    private RuneStorageRules() {}

    public static java.util.List<Integer> freePurchaseSlots(java.util.List<Integer> occupiedSlots) {
        java.util.List<Integer> free = new java.util.ArrayList<>();
        for (int slot = 0; slot < RuneLayoutDefinition.BOOST_TYPE_COUNT; slot++) {
            if (!occupiedSlots.contains(slot)) free.add(slot);
        }
        return free;
    }
}
