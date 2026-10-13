package com.example.customguimod;

/** Central rank model for rune progression and awakening rules. */
final class RuneProgression {
    static final int NORMAL_RANK = 1;
    static final int IMPROVED_RANK = 2;
    static final int ADVANCED_RANK = 3;

    private RuneProgression() {}

    static int clampRank(int rank) {
        if (rank <= NORMAL_RANK) return NORMAL_RANK;
        if (rank >= ADVANCED_RANK) return ADVANCED_RANK;
        return rank;
    }

    static boolean isMaximumRank(int rank) {
        return clampRank(rank) >= ADVANCED_RANK;
    }

    static int nextRank(int currentRank) {
        int rank = clampRank(currentRank);
        return rank >= ADVANCED_RANK ? ADVANCED_RANK : rank + 1;
    }

    static String rankName(int rank) {
        int normalized = clampRank(rank);
        if (normalized == IMPROVED_RANK) return "Улучшенная";
        if (normalized == ADVANCED_RANK) return "Продвинутая";
        return "Обычная";
    }

    static boolean allAtLeast(int[] bestRanks, int rank) {
        if (bestRanks == null || bestRanks.length < RuneInventory.BOOST_TYPE_COUNT) return false;
        int required = clampRank(rank);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (clampRank(bestRanks[rune]) < required) return false;
        }
        return true;
    }

    static boolean canAwaken(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneInventory.BOOST_TYPE_COUNT) return false;
        if (runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) return false;

        int currentRank = clampRank(bestRanks[runeIndex]);
        return currentRank < ADVANCED_RANK && allAtLeast(bestRanks, currentRank);
    }
}
