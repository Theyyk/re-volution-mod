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
        check(before.stageRank() == RuneProgression.ADVANCED_RANK, "full advanced collection must use advanced purchase stage");
        check(before.lowerProgress() == RuneInventory.BOOST_TYPE_COUNT, "full advanced stage must contain all slots");

        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(ranks, 3);
        ranks[3] = decision.targetRank;
        RuneCollectionProgression after = RuneCollectionProgression.fromBestRanks(ranks);
        check(after.stageRank() == RuneProgression.IMPROVED_RANK,
                "revert from full advanced must expose the nested improved stage");
        check(after.lowerProgress() == 1, "the reverted rune is the one factual improved rune");
        check(after.higherRuneCount() == RuneInventory.BOOST_TYPE_COUNT - 1,
                "the other advanced runes apply higher-rank purchase pressure");
        check(after.coveredCount() == RuneInventory.BOOST_TYPE_COUNT,
                "revert does not open a free purchase slot; the cycle remains fully covered");
        check(after.isOwnedAtStage(3), "the reverted improved rune remains owned at the improved stage");
    }

    private static void repeatedRevertRestoreCycleHasNoHiddenProgressionCounter() {
        int[] ranks = full(RuneProgression.ADVANCED_RANK);

        RuneRankRevertDecision first = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = first.targetRank;
        RuneCollectionProgression reverted = RuneCollectionProgression.fromBestRanks(ranks);
        check(reverted.stageRank() == RuneProgression.IMPROVED_RANK, "first revert must derive improved stage");
        check(reverted.lowerProgress() == 1, "first revert must derive one improved rune");
        check(reverted.higherRuneCount() == RuneInventory.BOOST_TYPE_COUNT - 1,
                "first revert must derive 25 higher-rank runes");

        ranks[11] = RuneProgression.ADVANCED_RANK;
        RuneCollectionProgression restored = RuneCollectionProgression.fromBestRanks(ranks);
        check(restored.stageRank() == RuneProgression.ADVANCED_RANK, "restored collection returns to advanced stage");
        check(restored.lowerProgress() == RuneInventory.BOOST_TYPE_COUNT,
                "restoring the factual rank derives the full advanced stage again");

        RuneRankRevertDecision second = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = second.targetRank;
        RuneCollectionProgression revertedAgain = RuneCollectionProgression.fromBestRanks(ranks);
        check(revertedAgain.stageRank() == reverted.stageRank(), "repeated cycle must keep the same factual stage");
        check(revertedAgain.lowerProgress() == reverted.lowerProgress(),
                "repeated cycle must keep the same factual lower progress");
        check(revertedAgain.higherRuneCount() == reverted.higherRuneCount(),
                "repeated cycle must keep the same factual higher-rank pressure");
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
