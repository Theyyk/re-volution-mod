package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;
import com.example.revolution.rune.definition.RuneRank;

/** Central rank model for rune progression and awakening rules. */
public final class RuneProgression {
    /** Legacy alias retained for source compatibility. */
    public static final int NORMAL_RANK = RuneLayoutDefinition.NORMAL_RANK;
    public static final int ORDINARY_RANK = RuneLayoutDefinition.ORDINARY_RANK;
    public static final int IMPROVED_RANK = RuneLayoutDefinition.IMPROVED_RANK;
    public static final int ADVANCED_RANK = RuneLayoutDefinition.ADVANCED_RANK;
    public static final int MYTHIC_RANK = RuneLayoutDefinition.MYTHIC_RANK;
    public static final int ANCIENT_RANK = RuneLayoutDefinition.ANCIENT_RANK;

    private RuneProgression() {}

    /** Clamp values used by active gameplay; ANCIENT remains reserved in v2.3.0. */
    public static int clampRank(int rank) {
        if (rank <= ORDINARY_RANK) return ORDINARY_RANK;
        if (rank >= MYTHIC_RANK) return MYTHIC_RANK;
        return rank;
    }

    public static boolean isMaximumRank(int rank) {
        return clampRank(rank) >= MYTHIC_RANK;
    }

    public static int nextRank(int currentRank) {
        RuneRank rank = RuneRank.fromPersistentId(clampRank(currentRank));
        return rank == null ? ORDINARY_RANK : rank.nextGameplayRank().persistentId();
    }

    public static String rankName(int rank) {
        RuneRank normalized = RuneRank.fromPersistentId(clampRank(rank));
        return normalized == null ? RuneRank.ORDINARY.displayName() : normalized.displayName();
    }

    public static boolean allAtLeast(int[] bestRanks, int rank) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.SLOTS_PER_COLLECTION) return false;
        int required = clampRank(rank);
        for (int rune = 0; rune < RuneLayoutDefinition.SLOTS_PER_COLLECTION; rune++) {
            if (bestRanks[rune] < required) return false;
        }
        return true;
    }

    public static boolean canAwaken(int[] bestRanks, int runeIndex) {
        return RuneAwakeningDecision.evaluate(bestRanks, runeIndex).isAllowed();
    }
}
