package com.example.revolution.rune.rules;

import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Regression coverage for selected-rune one-step rank revert under nested progression. */
public final class RuneRankRevertTest {
    public static void main(String[] args) {
        selectedAdvancedRuneRevertsOneStepOnly();
        selectedMythicRuneRevertsToAdvanced();
        lowerAndInvalidRanksAreRejected();
        progressionIsAlwaysDerivedFromActualRanks();
        repeatedRevertRestoreCycleHasNoHiddenProgressionCounter();
        System.out.println("Rune rank revert contract passed");
    }

    @Test void selectedAdvancedRuneRevertsOneStep() { selectedAdvancedRuneRevertsOneStepOnly(); }
    @Test void selectedMythicRuneRevertsOneStep() { selectedMythicRuneRevertsToAdvanced(); }
    @Test void rejectsLowerAndInvalidRanks() { lowerAndInvalidRanksAreRejected(); }
    @Test void derivesProgressionAfterRevert() { progressionIsAlwaysDerivedFromActualRanks(); }
    @Test void abuseCycleHasNoHiddenCounter() { repeatedRevertRestoreCycleHasNoHiddenProgressionCounter(); }

    private static void selectedAdvancedRuneRevertsOneStepOnly() {
        int[] ranks = full(RuneProgression.ADVANCED_RANK);
        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(ranks, 7);

        check(decision.isAllowed(), "advanced selected rune must be revertable");
        check(decision.currentRank == RuneProgression.ADVANCED_RANK, "source rank must be advanced");
        check(decision.targetRank == RuneProgression.IMPROVED_RANK, "revert must move exactly one rank down");

        ranks[7] = decision.targetRank;
        for (int rune = 0; rune < ranks.length; rune++) {
            int expected = rune == 7 ? RuneProgression.IMPROVED_RANK : RuneProgression.ADVANCED_RANK;
            check(ranks[rune] == expected, "only selected rune may change rank");
        }
    }

    private static void selectedMythicRuneRevertsToAdvanced() {
        int[] ranks = full(RuneProgression.MYTHIC_RANK);
        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(ranks, 4);

        check(decision.isAllowed(), "Mythic selected rune must use the generic revert path");
        check(decision.currentRank == RuneProgression.MYTHIC_RANK, "source rank must be Mythic");
        check(decision.targetRank == RuneProgression.ADVANCED_RANK,
                "Mythic revert must move exactly one rank down to Advanced");
        check(RuneRankRevertDecision.canRevertRank(RuneProgression.MYTHIC_RANK),
                "client eligibility must include Mythic through the same policy");
    }

    private static void lowerAndInvalidRanksAreRejected() {
        int[] ranks = full(RuneProgression.IMPROVED_RANK);
        check(RuneRankRevertDecision.evaluate(ranks, 0).reason == RuneRankRevertDecision.Reason.BELOW_ALLOWED_RANK,
                "improved -> ordinary player revert must stay forbidden");
        ranks[0] = RuneProgression.ORDINARY_RANK;
        check(RuneRankRevertDecision.evaluate(ranks, 0).reason == RuneRankRevertDecision.Reason.BELOW_ALLOWED_RANK,
                "ordinary rank must not be revertable");
        ranks[0] = 0;
        check(RuneRankRevertDecision.evaluate(ranks, 0).reason == RuneRankRevertDecision.Reason.MISSING_RUNE,
                "unowned rune must not be revertable");
        check(RuneRankRevertDecision.evaluate(ranks, -1).reason == RuneRankRevertDecision.Reason.INVALID_RUNE,
                "negative rune index must be rejected");
        check(RuneRankRevertDecision.evaluate(new int[1], 0).reason == RuneRankRevertDecision.Reason.INVALID_COLLECTION,
                "short collection state must be rejected");
    }

    private static void progressionIsAlwaysDerivedFromActualRanks() {
        int[] ranks = full(RuneProgression.ADVANCED_RANK);
        RuneCollectionProgression before = RuneCollectionProgression.fromBestRanks(ranks);
        check(before.stageRank() == RuneProgression.ORDINARY_RANK,
                "even full advanced collection must only allow COMMON purchases");
        check(before.lowerProgress() == 0, "no owned rune is COMMON");
        check(before.higherRuneCount() == RuneInventory.BOOST_TYPE_COUNT,
                "all 26 higher-rank runes cover COMMON slots");

        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(ranks, 3);
        ranks[3] = decision.targetRank;
        RuneCollectionProgression after = RuneCollectionProgression.fromBestRanks(ranks);
        check(after.stageRank() == RuneProgression.ORDINARY_RANK,
                "revert never unlocks purchases above COMMON");
        check(after.lowerProgress() == 0, "IMPROVED rune still covers a COMMON slot");
        check(after.higherRuneCount() == RuneInventory.BOOST_TYPE_COUNT,
                "all 26 runes remain above COMMON after revert");
        check(after.coveredCount() == RuneInventory.BOOST_TYPE_COUNT,
                "revert does not open a free purchase slot");
        check(after.isOwnedAtStage(3), "the reverted IMPROVED rune still occupies its factual slot");
    }

    private static void repeatedRevertRestoreCycleHasNoHiddenProgressionCounter() {
        int[] ranks = full(RuneProgression.ADVANCED_RANK);

        RuneRankRevertDecision first = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = first.targetRank;
        RuneCollectionProgression reverted = RuneCollectionProgression.fromBestRanks(ranks);
        check(reverted.stageRank() == RuneProgression.ORDINARY_RANK,
                "first revert must still grant only COMMON purchases");
        check(reverted.lowerProgress() == 0,
                "no COMMON rune appears merely by reverting ADVANCED to IMPROVED");
        check(reverted.higherRuneCount() == RuneInventory.BOOST_TYPE_COUNT,
                "all factual runes continue to cover their COMMON slots");

        ranks[11] = RuneProgression.ADVANCED_RANK;
        RuneCollectionProgression restored = RuneCollectionProgression.fromBestRanks(ranks);
        check(restored.stageRank() == RuneProgression.ORDINARY_RANK,
                "restored collection still grants COMMON only");
        check(restored.coveredCount() == RuneInventory.BOOST_TYPE_COUNT,
                "restoring does not change occupied slot count");

        RuneRankRevertDecision second = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = second.targetRank;
        RuneCollectionProgression revertedAgain = RuneCollectionProgression.fromBestRanks(ranks);
        check(revertedAgain.stageRank() == reverted.stageRank(),
                "repeated cycle retains COMMON-only purchases");
        check(revertedAgain.lowerProgress() == reverted.lowerProgress(),
                "repeated cycle does not invent COMMON runes");
        check(revertedAgain.higherRuneCount() == reverted.higherRuneCount(),
                "repeated cycle retains factual slot coverage");
    }

    private static int[] full(int rank) {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        Arrays.fill(ranks, rank);
        return ranks;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
