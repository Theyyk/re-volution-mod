package com.example.customguimod;

/** Regression checks for the shared rune progression rules. */
final class RuneProgressionTest {
    private RuneProgressionTest() {}

    static void run() {
        check(RuneProgression.NORMAL_RANK == 1, "Normal rank id must stay compatible with saved data");
        check(RuneProgression.IMPROVED_RANK == 2, "Improved rank id must stay compatible with saved data");
        check(RuneProgression.ADVANCED_RANK == 3, "Advanced rank id must stay compatible with saved data");

        check("Обычная".equals(RuneProgression.rankName(RuneProgression.NORMAL_RANK)),
                "Rank 1 must be named Обычная");
        check("Улучшенная".equals(RuneProgression.rankName(RuneProgression.IMPROVED_RANK)),
                "Rank 2 must be named Улучшенная");
        check("Продвинутая".equals(RuneProgression.rankName(RuneProgression.ADVANCED_RANK)),
                "Rank 3 must be named Продвинутая");

        check(RuneProgression.nextRank(RuneProgression.NORMAL_RANK) == RuneProgression.IMPROVED_RANK,
                "Обычная must progress to Улучшенная");
        check(RuneProgression.nextRank(RuneProgression.IMPROVED_RANK) == RuneProgression.ADVANCED_RANK,
                "Улучшенная must progress to Продвинутая");
        check(RuneProgression.nextRank(RuneProgression.ADVANCED_RANK) == RuneProgression.ADVANCED_RANK,
                "Продвинутая must be the maximum rank");

        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        check(!RuneProgression.canAwaken(ranks, 0),
                "Missing runes must not be treated as normal-rank runes");
        check(!RuneProgression.allAtLeast(ranks, RuneProgression.NORMAL_RANK),
                "An incomplete collection must not satisfy normal-rank progression");

        java.util.Arrays.fill(ranks, RuneProgression.NORMAL_RANK);
        check(RuneProgression.canAwaken(ranks, 0),
                "A full normal collection must allow awakening a normal rune");

        ranks[0] = RuneProgression.IMPROVED_RANK;
        check(!RuneProgression.canAwaken(ranks, 0),
                "An improved rune must wait until every rune reaches improved rank");

        java.util.Arrays.fill(ranks, RuneProgression.IMPROVED_RANK);
        check(RuneProgression.canAwaken(ranks, 0),
                "A full improved collection must allow awakening to advanced");

        ranks[0] = RuneProgression.ADVANCED_RANK;
        check(!RuneProgression.canAwaken(ranks, 0),
                "Advanced runes must not awaken further");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
