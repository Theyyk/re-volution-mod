package com.example.revolution.rune.definition;

/** Immutable data-driven rule for one forward rune rank transition. */
public final class RuneRankUpRule {
    public enum ClearMode {
        AT_OR_BELOW_SOURCE
    }

    public final int sourceRank;
    public final int targetRank;
    public final int requiredSlots;
    public final String resource;
    public final int cost;
    public final ClearMode clearMode;
    public final boolean preserveEnhancements;

    public RuneRankUpRule(
            int sourceRank,
            int targetRank,
            int requiredSlots,
            String resource,
            int cost,
            ClearMode clearMode,
            boolean preserveEnhancements) {
        this.sourceRank = sourceRank;
        this.targetRank = targetRank;
        this.requiredSlots = requiredSlots;
        this.resource = resource;
        this.cost = cost;
        this.clearMode = clearMode;
        this.preserveEnhancements = preserveEnhancements;
    }
}
