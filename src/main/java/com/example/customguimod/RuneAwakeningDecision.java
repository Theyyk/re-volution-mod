package com.example.customguimod;

/** Server/client-neutral result of checking whether a rune may be awakened. */
final class RuneAwakeningDecision {
    enum Reason {
        ALLOWED,
        INVALID_COLLECTION,
        INVALID_RUNE,
        MISSING_RUNE,
        MAXIMUM_RANK,
        COLLECTION_BEHIND
    }

    final Reason reason;
    final int currentRank;
    final int nextRank;

    private RuneAwakeningDecision(Reason reason, int currentRank, int nextRank) {
        this.reason = reason;
        this.currentRank = currentRank;
        this.nextRank = nextRank;
    }

    static RuneAwakeningDecision evaluate(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneInventory.BOOST_TYPE_COUNT) {
            return blocked(Reason.INVALID_COLLECTION, 0);
        }
        if (runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) {
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

    boolean isAllowed() {
        return reason == Reason.ALLOWED;
    }

    private static RuneAwakeningDecision blocked(Reason reason, int currentRank) {
        return new RuneAwakeningDecision(reason, currentRank, currentRank);
    }
}
