package com.example.customguimod;

/** Pure Java regression checks for multi-status deck routing. */
public final class RuneStatusRegistryTest {
    public static void main(String[] args) {
        run();
        System.out.println("Rune status routing checks passed");
    }

    static void run() {
        clearProperties();

        check(RuneStatusRegistry.statusForCollection(0) == RuneStatusRegistry.STATUS_POISON,
                "Deck 0 must remain Poison by default");
        check(RuneStatusRegistry.statusForCollection(1) == RuneStatusRegistry.STATUS_NONE,
                "Legacy decks must remain non-rune by default");
        check(RuneStatusRegistry.isRuneStatus(RuneStatusRegistry.STATUS_POISON),
                "Poison must be recognized as a rune status");
        check(!RuneStatusRegistry.isRuneStatus(RuneStatusRegistry.STATUS_NONE),
                "STATUS_NONE must not be treated as a rune status");
        check("poison".equals(RuneStatusRegistry.statusKey(RuneStatusRegistry.STATUS_POISON)),
                "Poison must keep its stable configuration key");
        check("Яд".equals(RuneStatusRegistry.displayNameForCollection(0, "1")),
                "Built-in Poison must use the user-facing Яд label regardless of stored legacy deck name");
        check("Legacy".equals(RuneStatusRegistry.displayNameForCollection(1, "Legacy")),
                "Legacy collections must keep their stored display name");

        System.setProperty("customguimod.runes.deck.1.status", "2");
        check(RuneStatusRegistry.statusForCollection(1) == 2,
                "A second deck must be able to opt into a separate rune status");
        check("status_2".equals(RuneStatusRegistry.statusKey(2)),
                "Unknown status ids must get deterministic default keys");
        check("Проклятие".equals(RuneStatusRegistry.displayNameForCollection(1, "Проклятие")),
                "Configured non-Poison statuses must keep their stored display name");

        System.setProperty("customguimod.runes.status.2.key", "Toxic-Frost");
        check("toxic_frost".equals(RuneStatusRegistry.statusKey(2)),
                "Custom status keys must be normalized for property paths");

        System.setProperty("customguimod.runes.deck.2.status", "bad");
        check(RuneStatusRegistry.statusForCollection(2) == RuneStatusRegistry.STATUS_NONE,
                "Invalid non-primary mappings must fall back to legacy behavior");

        System.setProperty("customguimod.runes.deck.0.status", "bad");
        check(RuneStatusRegistry.statusForCollection(0) == RuneStatusRegistry.STATUS_POISON,
                "Invalid primary mapping must preserve the current Poison default");

        System.setProperty("customguimod.runes.deck.1.status", "0");
        check(RuneStatusRegistry.statusForCollection(1) == RuneStatusRegistry.STATUS_NONE,
                "Zero must explicitly keep a deck in legacy mode");

        clearProperties();
    }

    private static void clearProperties() {
        System.clearProperty("customguimod.runes.deck.0.status");
        System.clearProperty("customguimod.runes.deck.1.status");
        System.clearProperty("customguimod.runes.deck.2.status");
        System.clearProperty("customguimod.runes.status.2.key");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
