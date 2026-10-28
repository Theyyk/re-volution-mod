package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/** Pure selected-rune decision for one-step rank revert. */
public final class RuneRankRevertDecision {
    public enum Reason {
        ALLOWED,
        INVALID_COLLECTION,
        INVALID_RUNE,
        MISSING_RUNE,
        BELOW_ALLOWED_RANK
    }

    public final Reason reason;
    public final int currentRank;
    public final int targetRank;

    private RuneRankRevertDecision(Reason reason, int currentRank, int targetRank) {
        this.reason = reason;
        this.currentRank = currentRank;
        this.targetRank = targetRank;
    }

    public static RuneRankRevertDecision evaluate(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            return new RuneRankRevertDecision(Reason.INVALID_COLLECTION, 0, 0);
        }
        if (runeIndex < 0 || runeIndex >= RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            return new RuneRankRevertDecision(Reason.INVALID_RUNE, 0, 0);
        }

        int currentRank = bestRanks[runeIndex];
        if (currentRank <= 0) {
            return new RuneRankRevertDecision(Reason.MISSING_RUNE, 0, 0);
        }
        if (!canRevertRank(currentRank)) {
            return new RuneRankRevertDecision(Reason.BELOW_ALLOWED_RANK, currentRank, currentRank);
        }

        return new RuneRankRevertDecision(Reason.ALLOWED, currentRank, currentRank - 1);
    }

    /** Revert starts at ADVANCED. The one-step rule also remains valid for future higher rank ids. */
    public static boolean canRevertRank(int rank) {
        return rank >= RuneProgression.ADVANCED_RANK;
    }

    public boolean isAllowed() {
        return reason == Reason.ALLOWED;
    }
}
