package com.example.revolution.pet.definition;

/** Gameplay Pet ranks. ANCIENT is reserved and intentionally not reachable in v2.4.0. */
public enum PetRank {
    ORDINARY(0, "Обычный"),
    IMPROVED(1, "Улучшенный"),
    ADVANCED(2, "Продвинутый"),
    MYTHIC(3, "Мифический"),
    ANCIENT(4, "Древний");

    private final int progressionOrder;
    private final String displayName;

    PetRank(int progressionOrder, String displayName) {
        this.progressionOrder = progressionOrder;
        this.displayName = displayName;
    }

    public int progressionOrder() { return progressionOrder; }
    public String displayName() { return displayName; }
    public boolean isPersistentAcrossPrestige() { return progressionOrder >= ADVANCED.progressionOrder; }
    public boolean isGameplayMaximum() { return this == MYTHIC || this == ANCIENT; }

    public PetRank nextGameplayRank() {
        switch (this) {
            case ORDINARY: return IMPROVED;
            case IMPROVED: return ADVANCED;
            case ADVANCED: return MYTHIC;
            default: return this;
        }
    }

    public PetRank previousGameplayRank() {
        switch (this) {
            case MYTHIC: return ADVANCED;
            case ADVANCED: return IMPROVED;
            case IMPROVED: return ORDINARY;
            default: return this;
        }
    }

    public static PetRank parse(String raw) {
        if (raw == null) return ORDINARY;
        try { return valueOf(raw); }
        catch (IllegalArgumentException ignored) { return ORDINARY; }
    }
}
