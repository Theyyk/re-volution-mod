package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/** Storage-facing boundary for rune ranks and legacy-slot rules. */
public final class RuneStorageRules {
    /** Legacy alias retained for existing saved data and source compatibility. */
    public static final int NORMAL_RANK = RuneLayoutDefinition.NORMAL_RANK;
    public static final int ORDINARY_RANK = RuneLayoutDefinition.ORDINARY_RANK;
    public static final int IMPROVED_RANK = RuneLayoutDefinition.IMPROVED_RANK;
    public static final int ADVANCED_RANK = RuneLayoutDefinition.ADVANCED_RANK;
    public static final int MYTHIC_RANK = RuneLayoutDefinition.MYTHIC_RANK;
    public static final int MAX_GAMEPLAY_RANK = RuneLayoutDefinition.MAX_GAMEPLAY_RANK;

    private RuneStorageRules() {}

    public static int clampGameplayRank(int rank) {
        return Math.max(ORDINARY_RANK, Math.min(MAX_GAMEPLAY_RANK, rank));
    }

    public static java.util.List<Integer> freePurchaseSlots(java.util.List<Integer> occupiedSlots) {
        java.util.List<Integer> free = new java.util.ArrayList<>();
        for (int slot = 0; slot < RuneLayoutDefinition.SLOTS_PER_COLLECTION; slot++) {
            if (!occupiedSlots.contains(slot)) free.add(slot);
        }
        return free;
    }
}
