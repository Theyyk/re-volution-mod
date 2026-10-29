package com.example.revolution.rune.definition;

/** Shared catalog/storage dimensions and rank identifiers. No runtime ownership. */
public final class RuneLayoutDefinition {
    public static final int LEGACY_RECORD_CAPACITY = 50;

    public static final int RUNE_COLLECTION_COUNT = 7;
    public static final int SLOTS_PER_COLLECTION = 26;

    /** Legacy alias retained while existing collection-1 code is generalized. */
    public static final int BOOST_TYPE_COUNT = SLOTS_PER_COLLECTION;

    /** Legacy name retained for source compatibility; product terminology is ORDINARY. */
    public static final int NORMAL_RANK = RuneRank.ORDINARY.persistentId();
    public static final int ORDINARY_RANK = RuneRank.ORDINARY.persistentId();
    public static final int IMPROVED_RANK = RuneRank.IMPROVED.persistentId();
    public static final int ADVANCED_RANK = RuneRank.ADVANCED.persistentId();
    public static final int MYTHIC_RANK = RuneRank.MYTHIC.persistentId();
    public static final int ANCIENT_RANK = RuneRank.ANCIENT.persistentId();

    /** Highest rank normal v2.3.0 gameplay is allowed to create. */
    public static final int MAX_GAMEPLAY_RANK = MYTHIC_RANK;

    private RuneLayoutDefinition() {}
}
