package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/** Server/client-neutral result of checking whether a rune may be awakened. */
public final class RuneAwakeningDecision {
    public enum Reason {
        ALLOWED,
        INVALID_COLLECTION,
        INVALID_RUNE,
        MISSING_RUNE,
        MAXIMUM_RANK,
        COLLECTION_BEHIND
    }

    public final Reason reason;
    public final int currentRank;
    public final int nextRank;

    private RuneAwakeningDecision(Reason reason, int currentRank, int nextRank) {
        this.reason = reason;
        this.currentRank = currentRank;
        this.nextRank = nextRank;
    }

    public static RuneAwakeningDecision evaluate(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            return blocked(Reason.INVALID_COLLECTION, 0);
        }
        if (runeIndex < 0 || runeIndex >= RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            return blocked(Reason.INVALID_RUNE, 0);
        }

        int rawRank = bestRanks[runeIndex];
        if (rawRank < RuneProgression.NORMAL_RANK) {
            return blocked(Reason.MISSING_RUNE, 0);
        }

        int currentRank = RuneProgression.clampRank(rawRank);
        if (RuneProgression.isMaximumRank(currentRank)) {
            return blocked(Reason.MAXIMUM_RANK, currentRank);
        }
        if (!RuneProgression.allAtLeast(bestRanks, currentRank)) {
            return blocked(Reason.COLLECTION_BEHIND, currentRank);
        }

        return new RuneAwakeningDecision(
                Reason.ALLOWED,
                currentRank,
                RuneProgression.nextRank(currentRank)
        );
    }

    public boolean isAllowed() {
        return reason == Reason.ALLOWED;
    }

    private static RuneAwakeningDecision blocked(Reason reason, int currentRank) {
        return new RuneAwakeningDecision(reason, currentRank, currentRank);
    }
}
