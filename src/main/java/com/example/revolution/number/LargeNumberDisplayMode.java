package com.example.revolution.number;

/** Client-selectable presentation mode for large numeric values. */
public enum LargeNumberDisplayMode {
    SIMPLIFIED("Упрощенный"),
    FULL("Полный"),
    ENGINEERING("Инженерный");

    private final String displayName;

    LargeNumberDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
