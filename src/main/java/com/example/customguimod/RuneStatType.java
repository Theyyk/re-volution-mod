package com.example.customguimod;

import java.util.Locale;

/**
 * Stable identifiers for player-facing rune stat categories.
 * v1.2.10 expands the original click/poison foundation with resource content
 * while keeping formatting rules centralized and independent from GUI rendering.
 */
enum RuneStatType {
    CLICK_DAMAGE_FLAT("Урон Клика", DisplayKind.FLAT),
    POISON_DAMAGE_FLAT("Урон Яда", DisplayKind.FLAT),
    RESOURCE_REWARD_PERCENT("Добыча ресурсов", DisplayKind.PERCENT);

    enum DisplayKind {
        FLAT,
        MULTIPLIER,
        PERCENT
    }

    final String displayName;
    final DisplayKind displayKind;

    RuneStatType(String displayName, DisplayKind displayKind) {
        this.displayName = displayName;
        this.displayKind = displayKind;
    }

    String format(double value) {
        switch (displayKind) {
            case MULTIPLIER:
                return "×" + compact(value);
            case PERCENT:
                return "+" + compact(value) + "%";
            case FLAT:
            default:
                return "+" + compact(value);
        }
    }

    private static String compact(double value) {
        if (!Double.isFinite(value)) return "0";
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
