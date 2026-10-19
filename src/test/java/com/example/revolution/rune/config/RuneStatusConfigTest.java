package com.example.revolution.rune.config;

/** Pure Java regression checks for multi-status deck routing. */
public final class RuneStatusConfigTest {
    public static void main(String[] args) {
        run();
        System.out.println("Rune status routing checks passed");
    }

    public static void run() {
        clearProperties();

        check(RuneStatusConfig.statusForCollection(0) == RuneStatusConfig.STATUS_POISON,
                "Deck 0 must remain Poison by default");
        check(RuneStatusConfig.statusForCollection(1) == RuneStatusConfig.STATUS_NONE,
                "Legacy decks must remain non-rune by default");
        check(RuneStatusConfig.isRuneStatus(RuneStatusConfig.STATUS_POISON),
                "Poison must be recognized as a rune status");
        check(!RuneStatusConfig.isRuneStatus(RuneStatusConfig.STATUS_NONE),
                "STATUS_NONE must not be treated as a rune status");
        check("poison".equals(RuneStatusConfig.statusKey(RuneStatusConfig.STATUS_POISON)),
                "Poison must keep its stable configuration key");
        check("Яд".equals(RuneStatusConfig.displayNameForCollection(0, "1")),
                "Built-in Poison must use the user-facing Яд label regardless of stored legacy deck name");
        check("Legacy".equals(RuneStatusConfig.displayNameForCollection(1, "Legacy")),
                "Legacy collections must keep their stored display name");

        System.setProperty("revolution.runes.deck.1.status", "2");
        check(RuneStatusConfig.statusForCollection(1) == 2,
                "A second deck must be able to opt into a separate rune status");
        check("status_2".equals(RuneStatusConfig.statusKey(2)),
                "Unknown status ids must get deterministic default keys");
        check("Проклятие".equals(RuneStatusConfig.displayNameForCollection(1, "Проклятие")),
                "Configured non-Poison statuses must keep their stored display name");

        System.setProperty("revolution.runes.status.2.key", "Toxic-Frost");
        check("toxic_frost".equals(RuneStatusConfig.statusKey(2)),
                "Custom status keys must be normalized for property paths");

        System.setProperty("revolution.runes.deck.2.status", "bad");
        check(RuneStatusConfig.statusForCollection(2) == RuneStatusConfig.STATUS_NONE,
                "Invalid non-primary mappings must fall back to legacy behavior");

        System.setProperty("revolution.runes.deck.0.status", "bad");
        check(RuneStatusConfig.statusForCollection(0) == RuneStatusConfig.STATUS_POISON,
                "Invalid primary mapping must preserve the current Poison default");

        System.setProperty("revolution.runes.deck.1.status", "0");
        check(RuneStatusConfig.statusForCollection(1) == RuneStatusConfig.STATUS_NONE,
                "Zero must explicitly keep a deck in legacy mode");

        clearProperties();
    }

    private static void clearProperties() {
        System.clearProperty("revolution.runes.deck.0.status");
        System.clearProperty("revolution.runes.deck.1.status");
        System.clearProperty("revolution.runes.deck.2.status");
        System.clearProperty("revolution.runes.status.2.key");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
