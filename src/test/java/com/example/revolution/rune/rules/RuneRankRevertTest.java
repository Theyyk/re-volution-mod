package com.example.revolution.rune.rules;

import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for v1.4.9 selected-rune one-step rank revert. */
public final class RuneRankRevertTest {
    public static void main(String[] args) {
        selectedAdvancedRuneRevertsOneStepOnly();
        lowerAndInvalidRanksAreRejected();
        progressionIsAlwaysDerivedFromActualRanks();
        repeatedRevertRestoreCycleHasNoHiddenProgressionCounter();
        System.out.println("Rune rank revert contract passed");
    }

    @Test void selectedAdvancedRuneRevertsOneStep() { selectedAdvancedRuneRevertsOneStepOnly(); }
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

    private static void lowerAndInvalidRanksAreRejected() {
        int[] ranks = full(RuneProgression.IMPROVED_RANK);
        check(RuneRankRevertDecision.evaluate(ranks, 0).reason == RuneRankRevertDecision.Reason.BELOW_ALLOWED_RANK,
                "improved -> normal player revert must stay forbidden");
        ranks[0] = RuneProgression.NORMAL_RANK;
        check(RuneRankRevertDecision.evaluate(ranks, 0).reason == RuneRankRevertDecision.Reason.BELOW_ALLOWED_RANK,
                "normal rank must not be revertable");
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
        check(before.progressionIndex() == RuneInventory.BOOST_TYPE_COUNT, "full advanced stage must contain all slots");

        RuneRankRevertDecision decision = RuneRankRevertDecision.evaluate(ranks, 3);
        ranks[3] = decision.targetRank;
        RuneCollectionProgression after = RuneCollectionProgression.fromBestRanks(ranks);
        check(after.stageRank() == RuneProgression.ADVANCED_RANK,
                "other advanced runes keep the factual active purchase stage at advanced");
        check(after.progressionIndex() == RuneInventory.BOOST_TYPE_COUNT - 1,
                "reverted rune must immediately disappear from advanced-stage progression");
        check(!after.isOwnedAtStage(3), "reverted rune must become the factual missing advanced-stage slot");
    }

    private static void repeatedRevertRestoreCycleHasNoHiddenProgressionCounter() {
        int[] ranks = full(RuneProgression.ADVANCED_RANK);

        RuneRankRevertDecision first = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = first.targetRank;
        RuneCollectionProgression reverted = RuneCollectionProgression.fromBestRanks(ranks);
        check(reverted.progressionIndex() == RuneInventory.BOOST_TYPE_COUNT - 1,
                "first revert must derive index 25 from ranks only");

        // A later purchase or fully paid rank-up may restore the factual selected rank.
        ranks[11] = RuneProgression.ADVANCED_RANK;
        RuneCollectionProgression restored = RuneCollectionProgression.fromBestRanks(ranks);
        check(restored.progressionIndex() == RuneInventory.BOOST_TYPE_COUNT,
                "restoring the factual rank must derive the full stage again");

        // Reverting again must return to exactly the same progression point, not decrement hidden state.
        RuneRankRevertDecision second = RuneRankRevertDecision.evaluate(ranks, 11);
        ranks[11] = second.targetRank;
        RuneCollectionProgression revertedAgain = RuneCollectionProgression.fromBestRanks(ranks);
        check(revertedAgain.stageRank() == reverted.stageRank(), "repeated cycle must keep the same factual stage");
        check(revertedAgain.progressionIndex() == reverted.progressionIndex(),
                "repeated cycle must not accumulate a hidden purchase-index delta");
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
