package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneLayoutDefinition;

/**
 * Derived purchase progression for one rune collection.
 *
 * <p>The factual per-rune ranks are the source of truth. No purchase index or collection rank is
 * persisted independently. This is intentionally suitable for future local rank reverts: changing
 * one rune rank changes the derived stage population on the next calculation.</p>
 */
public final class RuneCollectionProgression {
    private final int stageRank;
    private final int progressionIndex;
    private final boolean[] ownedAtStage;

    private RuneCollectionProgression(int stageRank, int progressionIndex, boolean[] ownedAtStage) {
        this.stageRank = stageRank;
        this.progressionIndex = progressionIndex;
        this.ownedAtStage = ownedAtStage;
    }

    public static RuneCollectionProgression fromBestRanks(int[] bestRanks) {
        if (bestRanks == null || bestRanks.length < RuneLayoutDefinition.BOOST_TYPE_COUNT) {
            throw new IllegalArgumentException("bestRanks must contain all rune slots");
        }

        int stageRank = RuneProgression.NORMAL_RANK;
        boolean hasRune = false;
        for (int rune = 0; rune < RuneLayoutDefinition.BOOST_TYPE_COUNT; rune++) {
            if (bestRanks[rune] <= 0) continue;
            hasRune = true;
            stageRank = Math.max(stageRank, RuneProgression.clampRank(bestRanks[rune]));
        }
        if (!hasRune) stageRank = RuneProgression.NORMAL_RANK;

        boolean[] ownedAtStage = new boolean[RuneLayoutDefinition.BOOST_TYPE_COUNT];
        int progressionIndex = 0;
        for (int rune = 0; rune < RuneLayoutDefinition.BOOST_TYPE_COUNT; rune++) {
            int rawRank = bestRanks[rune];
            if (rawRank <= 0) continue;
            int rank = RuneProgression.clampRank(rawRank);
            if (rank < stageRank) continue;
            ownedAtStage[rune] = true;
            progressionIndex++;
        }

        return new RuneCollectionProgression(stageRank, progressionIndex, ownedAtStage);
    }

    public int stageRank() {
        return stageRank;
    }

    /** Number of factual runes that currently belong to the active purchase stage. */
    public int progressionIndex() {
        return progressionIndex;
    }

    public boolean isOwnedAtStage(int runeIndex) {
        return runeIndex >= 0 && runeIndex < ownedAtStage.length && ownedAtStage[runeIndex];
    }
}
