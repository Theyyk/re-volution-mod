package com.example.customguimod;

/** Client-selectable presentation mode for values above centillion. */
enum LargeNumberDisplayMode {
    FULL("Полный"),
    ABBREVIATED("Укороченный"),
    SHORT("Короткий"),
    SCIENTIFIC("Научный");

    private final String displayName;

    LargeNumberDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }
}
