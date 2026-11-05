package com.example.revolution.client.ui;

import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;

/** Semantic E-menu sections and a single source for button IDs. */
public enum UiMenuSection {
    RUNES("runes", 200, "Р", "Руны", null),
    UNIQUE_RUNES("unique_runes", 300, "У", "Уникальные руны", BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS),
    PETS("pets", 301, "П", "Питомцы", null),
    MECHANIC_UNLOCKS("mechanic_unlocks", 302, "О", "Разблокировки", null),
    ACHIEVEMENTS("achievements", 303, "Д", "Достижения", BalanceMechanicUnlockPolicy.ACHIEVEMENTS);

    private final String id;
    private final int buttonId;
    private final String shortLabel;
    private final String displayName;
    private final String mechanicUnlockId;

    UiMenuSection(String id, int buttonId, String shortLabel, String displayName, String mechanicUnlockId) {
        this.id = id;
        this.buttonId = buttonId;
        this.shortLabel = shortLabel;
        this.displayName = displayName;
        this.mechanicUnlockId = mechanicUnlockId;
    }

    public String id() {
        return id;
    }

    public int buttonId() {
        return buttonId;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public String displayName() {
        return displayName;
    }

    /** Null means no v2.12.0 permanent mechanic unlock gates this tab. */
    public String mechanicUnlockId() {
        return mechanicUnlockId;
    }

    public static UiMenuSection fromId(String id) {
        for (UiMenuSection section : values()) if (section.id.equals(id)) return section;
        throw new IllegalArgumentException("Unknown UI section: " + id);
    }

    public static UiMenuSection fromButtonId(int buttonId) {
        for (UiMenuSection section : values()) if (section.buttonId == buttonId) return section;
        return null;
    }

    /** Main E navigation excludes unique runes, which live in the Rune collections strip. */
    public static UiMenuSection[] mainTabs() {
        return new UiMenuSection[] {RUNES, PETS, MECHANIC_UNLOCKS, ACHIEVEMENTS};
    }

    public static UiMenuSection[] auxiliaryTabs() {
        return new UiMenuSection[] {PETS, MECHANIC_UNLOCKS, ACHIEVEMENTS};
    }
}
