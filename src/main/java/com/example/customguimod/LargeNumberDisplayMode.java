package com.example.customguimod;

/** Client-selectable presentation mode for large numeric values. */
enum LargeNumberDisplayMode {
    LETTER("Буквенный"),
    ENGINEERING("Инженерный");

    private final String displayName;

    LargeNumberDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }
}
