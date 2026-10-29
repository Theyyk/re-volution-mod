package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/**
 * Derived nested purchase progression for one rune collection.
 *
 * <p>The factual per-rune ranks are the only source of truth. A higher-rank rune occupies its slot
 * for the current lower-rank rebuild, while purchases continue one rank below the factual maximum
 * until all 26 slots reach that maximum rank.</p>
 */
public final class RuneCollectionProgression {
    private final int stageRank;
    private final int lowerProgress;
    private final int higherRuneCount;
    private final boolean[] ownedAtStage;

    private RuneCollectionProgression(
            int stageRank,
            int lowerProgress,
            int higherRuneCount,
            boolean[] ownedAtStage) {
        this.stageRank = stageRank;
        this.lowerProgress = lowerProgress;
        this.higherRuneCount = higherRuneCount;
        this.ownedAtStage = ownedAtStage;
    }

    public static RuneCollectionProgression fromBestRanks(int[] bestRanks) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            throw new IllegalArgumentException("bestRanks must contain all rune slots");
        }

        int highestRank = RuneProgression.NORMAL_RANK;
        int highestRankCount = 0;
        for (int rune = 0; rune < RuneLayoutDefinition.BOOST_TYPE_COUNT; rune++) {
            int rawRank = bestRanks[rune];
            if (rawRank <= 0) continue;
            int rank = RuneProgression.clampRank(rawRank);
            if (rank > highestRank) {
                highestRank = rank;
                highestRankCount = 1;
            } else if (rank == highestRank) {
                highestRankCount++;
            }
        }

        int stageRank;
        if (highestRank <= RuneProgression.NORMAL_RANK) {
            stageRank = RuneProgression.NORMAL_RANK;
        } else if (highestRankCount >= RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            stageRank = highestRank;
        } else {
            stageRank = Math.max(RuneProgression.NORMAL_RANK, highestRank - 1);
        }

        boolean[] ownedAtStage = new boolean[RuneLayoutDefinition.BOOST_TYPE_COUNT];
        int lowerProgress = 0;
        int higherRuneCount = 0;
        for (int rune = 0; rune < RuneLayoutDefinition.BOOST_TYPE_COUNT; rune++) {
            int rawRank = bestRanks[rune];
            if (rawRank <= 0) continue;
            int rank = RuneProgression.clampRank(rawRank);
            if (rank < stageRank) continue;
            ownedAtStage[rune] = true;
            if (rank == stageRank) lowerProgress++;
            else higherRuneCount++;
        }

        return new RuneCollectionProgression(stageRank, lowerProgress, higherRuneCount, ownedAtStage);
    }

    public int stageRank() {
        return stageRank;
    }

    /** Number of factual runes exactly at the rank currently being rebuilt. */
    public int lowerProgress() {
        return lowerProgress;
    }

    /** Number of factual runes already above the rank currently being rebuilt. */
    public int higherRuneCount() {
        return higherRuneCount;
    }

    /** Number of slots already covered at or above the current purchase rank. */
    public int coveredCount() {
        return lowerProgress + higherRuneCount;
    }

    /** Compatibility alias: purchase curve index is the factual lower-rank progress. */
    public int progressionIndex() {
        return lowerProgress;
    }

    public boolean isComplete() {
        return coveredCount() >= RuneLayoutDefinition.BOOST_TYPE_COUNT;
    }

    public boolean isOwnedAtStage(int runeIndex) {
        return runeIndex >= 0 && runeIndex < ownedAtStage.length && ownedAtStage[runeIndex];
    }
}
