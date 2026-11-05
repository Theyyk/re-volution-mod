package com.example.revolution.client.ui;

/**
 * Stable semantic section IDs, independent of Minecraft GuiButton numeric IDs.
 * UI-02 will attach these sections to the existing rune screen without changing
 * the rune collection layout or its left-side stats.
 */
public enum UiMenuSection {
    RUNES("runes"),
    UNIQUE_RUNES("unique_runes"),
    PETS("pets"),
    MECHANIC_UNLOCKS("mechanic_unlocks"),
    ACHIEVEMENTS("achievements");

    private final String id;

    UiMenuSection(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static UiMenuSection fromId(String id) {
        for (UiMenuSection section : values()) {
            if (section.id.equals(id)) return section;
        }
        throw new IllegalArgumentException("Unknown UI section: " + id);
    }
}
