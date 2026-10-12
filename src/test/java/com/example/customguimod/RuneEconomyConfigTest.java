package com.example.customguimod;

/** Pure Java regression checks for configurable duplicate compensation rules. */
public final class RuneEconomyConfigTest {
    public static void main(String[] args) {
        run();
        System.out.println("Rune economy fixed, formula and event modifier checks passed");
    }

    static void run() {
        clearProperties();

        check(ResourceManager.RUNE_DUST.equals(
                        RuneEconomyConfig.resourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Poison status must use rune dust");
        check(RuneEconomyConfig.duplicateCalculationType(RuneEconomyConfig.STATUS_POISON)
                        == RuneEconomyConfig.CalculationType.FIXED,
                "Poison duplicate compensation must default to FIXED");
        check(RuneEconomyConfig.duplicateCompensationPerRune(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 10,
                "Poison duplicate compensation must default to 10 rune dust");
        check(RuneEconomyConfig.duplicateCompensation(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK, 3) == 30,
                "Three poison duplicates must compensate 30 rune dust");

        System.setProperty("customguimod.runes.poison.duplicate.fixed", "15");
        check(RuneEconomyConfig.duplicateCompensation(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK, 2) == 30,
                "Fixed compensation must be adjustable without purchase-logic changes");

        System.setProperty("customguimod.runes.poison.duplicate.type", "FORMULA");
        System.setProperty("customguimod.runes.poison.duplicate.formula.base", "10");
        System.setProperty("customguimod.runes.poison.duplicate.formula.per_rank", "5");
        check(RuneEconomyConfig.duplicateCompensationPerRune(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.SILVER_RANK) == 15,
                "Formula mode must support rank-based growth");

        System.setProperty("customguimod.runes.event.multiplier", "2.0");
        check(RuneEconomyConfig.duplicateCompensationPerRune(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 20,
                "Event multiplier must modify the base calculation without replacing it");

        clearProperties();
    }

    private static void clearProperties() {
        System.clearProperty("customguimod.runes.poison.duplicate.type");
        System.clearProperty("customguimod.runes.poison.duplicate.fixed");
        System.clearProperty("customguimod.runes.poison.duplicate.formula.base");
        System.clearProperty("customguimod.runes.poison.duplicate.formula.per_rank");
        System.clearProperty("customguimod.runes.poison.duplicate.rank.1");
        System.clearProperty("customguimod.runes.poison.duplicate.rank.2");
        System.clearProperty("customguimod.runes.poison.duplicate.rank.3");
        System.clearProperty("customguimod.runes.event.multiplier");
        System.clearProperty("customguimod.runes.poison.event.multiplier");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
