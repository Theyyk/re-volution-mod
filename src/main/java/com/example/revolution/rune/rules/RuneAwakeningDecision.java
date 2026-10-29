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

    /** Default rule for the shared v2.3.0 gameplay progression through MYTHIC. */
    public static RuneAwakeningDecision evaluate(int[] bestRanks, int runeIndex) {
        int currentRank = rawCurrentRank(bestRanks, runeIndex);
        if (currentRank <= 0) return basicValidationFailure(bestRanks, runeIndex);
        return evaluate(bestRanks, runeIndex, RuneProgression.nextRank(currentRank), RuneLayoutDefinition.SLOTS_PER_COLLECTION);
    }

    /** Evaluate one data-driven forward transition without persisting collection-level progression state. */
    public static RuneAwakeningDecision evaluate(
            int[] bestRanks,
            int runeIndex,
            int targetRank,
            int requiredSlots) {
        RuneAwakeningDecision basicFailure = basicValidationFailure(bestRanks, runeIndex);
        if (basicFailure != null) return basicFailure;

        int currentRank = RuneProgression.clampRank(bestRanks[runeIndex]);
        if (RuneProgression.isMaximumRank(currentRank)) {
            return blocked(Reason.MAXIMUM_RANK, currentRank);
        }
        if (targetRank <= currentRank || targetRank > RuneProgression.MYTHIC_RANK) {
            return blocked(Reason.MAXIMUM_RANK, currentRank);
        }
        if (requiredSlots <= 0 || requiredSlots > RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            return blocked(Reason.INVALID_COLLECTION, currentRank);
        }

        int readySlots = 0;
        for (int rune = 0; rune < RuneLayoutDefinition.SLOTS_PER_COLLECTION; rune++) {
            if (bestRanks[rune] >= currentRank) readySlots++;
        }
        if (readySlots < requiredSlots) {
            return blocked(Reason.COLLECTION_BEHIND, currentRank);
        }

        return new RuneAwakeningDecision(Reason.ALLOWED, currentRank, targetRank);
    }

    public boolean isAllowed() {
        return reason == Reason.ALLOWED;
    }

    private static RuneAwakeningDecision basicValidationFailure(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            return blocked(Reason.INVALID_COLLECTION, 0);
        }
        if (runeIndex < 0 || runeIndex >= RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            return blocked(Reason.INVALID_RUNE, 0);
        }
        if (bestRanks[runeIndex] < RuneProgression.ORDINARY_RANK) {
            return blocked(Reason.MISSING_RUNE, 0);
        }
        return null;
    }

    private static int rawCurrentRank(int[] bestRanks, int runeIndex) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.SLOTS_PER_COLLECTION) return 0;
        if (runeIndex < 0 || runeIndex >= RuneLayoutDefinition.SLOTS_PER_COLLECTION) return 0;
        return bestRanks[runeIndex];
    }

    private static RuneAwakeningDecision blocked(Reason reason, int currentRank) {
        return new RuneAwakeningDecision(reason, currentRank, currentRank);
    }
}
