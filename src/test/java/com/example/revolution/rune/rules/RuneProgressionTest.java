package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;
import com.example.revolution.rune.definition.RuneRank;
import com.example.revolution.rune.state.RuneInventory;

/** Regression checks for the shared rune progression rules. */
public final class RuneProgressionTest {
    private RuneProgressionTest() {}

    public static void run() {
        check(RuneProgression.ORDINARY_RANK == 1, "Ordinary rank id must stay compatible with saved data");
        check(RuneProgression.NORMAL_RANK == RuneProgression.ORDINARY_RANK,
                "Legacy normal rank alias must remain compatible");
        check(RuneProgression.IMPROVED_RANK == 2, "Improved rank id must stay compatible with saved data");
        check(RuneProgression.ADVANCED_RANK == 3, "Advanced rank id must stay compatible with saved data");
        check(RuneProgression.MYTHIC_RANK == 4, "Mythic rank id must be 4");
        check(RuneProgression.ANCIENT_RANK == 5, "Ancient reserve rank id must be 5");
        check(RuneLayoutDefinition.RUNE_COLLECTION_COUNT == 7, "v2.3.0 must define seven rune collections");
        check(RuneLayoutDefinition.SLOTS_PER_COLLECTION == 26, "Every rune collection must have 26 slots");
        check(!RuneRank.ANCIENT.gameplayEnabled(), "Ancient must remain reserved in v2.3.0");

        check("Обычная".equals(RuneProgression.rankName(RuneProgression.ORDINARY_RANK)),
                "Rank 1 must be named Обычная");
        check("Улучшенная".equals(RuneProgression.rankName(RuneProgression.IMPROVED_RANK)),
                "Rank 2 must be named Улучшенная");
        check("Продвинутая".equals(RuneProgression.rankName(RuneProgression.ADVANCED_RANK)),
                "Rank 3 must be named Продвинутая");
        check("Мифическая".equals(RuneProgression.rankName(RuneProgression.MYTHIC_RANK)),
                "Rank 4 must be named Мифическая");

        check(RuneProgression.nextRank(RuneProgression.ORDINARY_RANK) == RuneProgression.IMPROVED_RANK,
                "Обычная must progress to Улучшенная");
        check(RuneProgression.nextRank(RuneProgression.IMPROVED_RANK) == RuneProgression.ADVANCED_RANK,
                "Улучшенная must progress to Продвинутая");
        check(RuneProgression.nextRank(RuneProgression.ADVANCED_RANK) == RuneProgression.MYTHIC_RANK,
                "Продвинутая must progress to Мифическая");
        check(RuneProgression.nextRank(RuneProgression.MYTHIC_RANK) == RuneProgression.MYTHIC_RANK,
                "Мифическая must be the maximum gameplay rank");

        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        check(!RuneProgression.canAwaken(ranks, 0),
                "Missing runes must not be treated as ordinary-rank runes");
        check(!RuneProgression.allAtLeast(ranks, RuneProgression.ORDINARY_RANK),
                "An incomplete collection must not satisfy ordinary-rank progression");
        check(RuneAwakeningDecision.evaluate(ranks, 0).reason == RuneAwakeningDecision.Reason.MISSING_RUNE,
                "Awakening decision must expose missing-rune state");
        check(RuneAwakeningDecision.evaluate(new int[1], 0).reason
                        == RuneAwakeningDecision.Reason.INVALID_COLLECTION,
                "Short rank arrays must be rejected as invalid collections");
        check(RuneAwakeningDecision.evaluate(ranks, -1).reason == RuneAwakeningDecision.Reason.INVALID_RUNE,
                "Invalid rune indexes must be reported explicitly");

        java.util.Arrays.fill(ranks, RuneProgression.ORDINARY_RANK);
        check(RuneProgression.canAwaken(ranks, 0),
                "A full ordinary collection must allow awakening an ordinary rune");
        RuneAwakeningDecision ordinaryDecision = RuneAwakeningDecision.evaluate(ranks, 0);
        check(ordinaryDecision.isAllowed(), "A full ordinary collection must return an allowed decision");
        check(ordinaryDecision.currentRank == RuneProgression.ORDINARY_RANK,
                "Allowed ordinary awakening must report current ordinary rank");
        check(ordinaryDecision.nextRank == RuneProgression.IMPROVED_RANK,
                "Allowed ordinary awakening must target improved rank");

        ranks[0] = RuneProgression.IMPROVED_RANK;
        check(!RuneProgression.canAwaken(ranks, 0),
                "An improved rune must wait until every rune reaches improved rank");
        check(RuneAwakeningDecision.evaluate(ranks, 0).reason
                        == RuneAwakeningDecision.Reason.COLLECTION_BEHIND,
                "Awakening decision must expose collection-behind state");

        java.util.Arrays.fill(ranks, RuneProgression.IMPROVED_RANK);
        check(RuneProgression.canAwaken(ranks, 0),
                "A full improved collection must allow awakening to advanced");
        RuneAwakeningDecision improvedDecision = RuneAwakeningDecision.evaluate(ranks, 0);
        check(improvedDecision.nextRank == RuneProgression.ADVANCED_RANK,
                "Allowed improved awakening must target advanced rank");

        java.util.Arrays.fill(ranks, RuneProgression.ADVANCED_RANK);
        check(RuneProgression.canAwaken(ranks, 0),
                "A full advanced collection must allow awakening to Mythic");
        RuneAwakeningDecision advancedDecision = RuneAwakeningDecision.evaluate(ranks, 0);
        check(advancedDecision.nextRank == RuneProgression.MYTHIC_RANK,
                "Allowed advanced awakening must target Mythic rank");

        java.util.Arrays.fill(ranks, RuneProgression.MYTHIC_RANK);
        check(!RuneProgression.canAwaken(ranks, 0),
                "Mythic runes must not awaken into reserved Ancient gameplay");
        check(RuneAwakeningDecision.evaluate(ranks, 0).reason == RuneAwakeningDecision.Reason.MAXIMUM_RANK,
                "Awakening decision must expose maximum-rank state at Mythic");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
