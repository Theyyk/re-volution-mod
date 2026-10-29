package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/** Pure factual-state rules for the permanent rune collection unlock chain. */
public final class RuneCollectionUnlockRules {
    public static final int DEFAULT_THRESHOLD = 13;

    private RuneCollectionUnlockRules() {}

    public static int countImprovedOrHigher(int[] ranks) {
        if (ranks == null) return 0;
        int count = 0;
        int limit = Math.min(ranks.length, RuneLayoutDefinition.SLOTS_PER_COLLECTION);
        for (int i = 0; i < limit; i++) {
            if (ranks[i] >= RuneProgression.IMPROVED_RANK) count++;
        }
        return count;
    }

    public static boolean qualifies(int[] ranks, int threshold) {
        if (threshold <= 0 || threshold > RuneLayoutDefinition.SLOTS_PER_COLLECTION) return false;
        return countImprovedOrHigher(ranks) >= threshold;
    }
}
