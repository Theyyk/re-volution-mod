package com.example.revolution.rune.rules;

/** Pure rank policy for Prestige: lower ranks return to Ordinary, Advanced+ are preserved. */
public final class RunePrestigeResetRules {
    private RunePrestigeResetRules() {}

    public static boolean shouldReset(int currentRank) {
        return currentRank >= RuneProgression.ORDINARY_RANK
                && currentRank < RuneProgression.ADVANCED_RANK;
    }

    public static int rankAfterReset(int currentRank) {
        if (shouldReset(currentRank)) return RuneProgression.ORDINARY_RANK;
        return currentRank;
    }
}
