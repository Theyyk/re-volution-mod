package com.example.revolution.prestige;

/** Player-selected Mastery Reset execution mode. Balance Core decides whether premium is enabled and what it preserves. */
public enum MasteryResetMode {
    NORMAL,
    PREMIUM_PRESERVE;

    public static MasteryResetMode parse(String raw) {
        if (raw == null) return NORMAL;
        String value = raw.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        if (value.equals("PREMIUM") || value.equals("PRESERVE")) return PREMIUM_PRESERVE;
        return valueOf(value);
    }
}
