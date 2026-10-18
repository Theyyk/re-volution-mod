package com.example.customguimod.rune;
/** Central rank model for rune progression and awakening rules. */
public final class RuneProgression {
    public static final int NORMAL_RANK = 1;
    public static final int IMPROVED_RANK = 2;
    public static final int ADVANCED_RANK = 3;

    private RuneProgression() {}

    public static int clampRank(int rank) {
        if (rank <= NORMAL_RANK) return NORMAL_RANK;
        if (rank >= ADVANCED_RANK) return ADVANCED_RANK;
        return rank;
    }

    public static boolean isMaximumRank(int rank) {
        return clampRank(rank) >= ADVANCED_RANK;
    }

    public static int nextRank(int currentRank) {
        int rank = clampRank(currentRank);
        return rank >= ADVANCED_RANK ? ADVANCED_RANK : rank + 1;
    }

    public static String rankName(int rank) {
        int normalized = clampRank(rank);
        if (normalized == IMPROVED_RANK) return "Улучшенная";
        if (normalized == ADVANCED_RANK) return "Продвинутая";
        return "Обычная";
    }

    public static boolean allAtLeast(int[] bestRanks, int rank) {
        if (bestRanks == null || bestRanks.length < RuneInventory.BOOST_TYPE_COUNT) return false;
        int required = clampRank(rank);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (bestRanks[rune] < required) return false;
        }
        return true;
    }

    public static boolean canAwaken(int[] bestRanks, int runeIndex) {
        return RuneAwakeningDecision.evaluate(bestRanks, runeIndex).isAllowed();
    }
}
