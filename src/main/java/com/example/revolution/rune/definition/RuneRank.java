package com.example.revolution.rune.definition;

/**
 * Canonical rune rank identity.
 *
 * <p>Persistent ids 1..3 deliberately match the legacy saved-data contract.
 * MYTHIC is the highest gameplay rank in v2.3.0. ANCIENT is reserved only and
 * must not be produced by normal progression yet.</p>
 */
public enum RuneRank {
    ORDINARY(1, "Обычная", true),
    IMPROVED(2, "Улучшенная", true),
    ADVANCED(3, "Продвинутая", true),
    MYTHIC(4, "Мифическая", true),
    ANCIENT(5, "Древняя", false);

    private final int persistentId;
    private final String displayName;
    private final boolean gameplayEnabled;

    RuneRank(int persistentId, String displayName, boolean gameplayEnabled) {
        this.persistentId = persistentId;
        this.displayName = displayName;
        this.gameplayEnabled = gameplayEnabled;
    }

    public int persistentId() {
        return persistentId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean gameplayEnabled() {
        return gameplayEnabled;
    }

    public boolean isAtLeast(RuneRank other) {
        return persistentId >= other.persistentId;
    }

    public RuneRank nextGameplayRank() {
        switch (this) {
            case ORDINARY:
                return IMPROVED;
            case IMPROVED:
                return ADVANCED;
            case ADVANCED:
                return MYTHIC;
            default:
                return this;
        }
    }

    public RuneRank previousRank() {
        switch (this) {
            case IMPROVED:
                return ORDINARY;
            case ADVANCED:
                return IMPROVED;
            case MYTHIC:
                return ADVANCED;
            case ANCIENT:
                return MYTHIC;
            default:
                return this;
        }
    }

    public static RuneRank fromPersistentId(int id) {
        for (RuneRank rank : values()) {
            if (rank.persistentId == id) return rank;
        }
        return null;
    }
}
