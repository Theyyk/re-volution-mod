package com.example.customguimod;

/** Regression checks proving economy/progression inputs are isolated per rune status. */
public final class MultiStatusRuneEconomyTest {
    public static void main(String[] args) {
        run();
        System.out.println("Multi-status rune economy checks passed");
    }

    static void run() {
        clearProperties();

        final int secondStatus = 2;
        System.setProperty("customguimod.runes.status.2.key", "frost");

        check("coins".equals(RuneEconomyConfig.purchaseResourceForStatus(secondStatus)),
                "A new status must inherit the neutral coin purchase fallback");
        check(RuneEconomyConfig.purchaseUnitCost(secondStatus) == 100,
                "A new status must inherit the technical 100 purchase baseline");
        check(RuneEconomyConfig.resourceForStatus(secondStatus).isEmpty(),
                "A new status must not inherit Poison duplicate resource");
        check(RuneEconomyConfig.duplicateCompensationPerRune(
                        secondStatus, RuneInventory.NORMAL_RANK) == 0,
                "A new status must not inherit Poison duplicate compensation");
        check(!RuneEconomyConfig.isAwakeningPaymentConfigured(
                        secondStatus, RuneInventory.NORMAL_RANK),
                "A new status must not inherit Poison paid awakening");

        System.setProperty("customguimod.runes.frost.purchase.resource", "crystals");
        System.setProperty("customguimod.runes.frost.purchase.cost", "35");
        check("crystals".equals(RuneEconomyConfig.purchaseResourceForStatus(secondStatus)),
                "Second status purchase resource must be independent");
        check(RuneEconomyConfig.purchaseCost(secondStatus, 5) == 175,
                "Second status purchase price must be independent");

        System.setProperty("customguimod.runes.frost.duplicate.resource", "rune_shards");
        System.setProperty("customguimod.runes.frost.duplicate.fixed", "7");
        check(ResourceManager.RUNE_SHARDS.equals(RuneEconomyConfig.resourceForStatus(secondStatus)),
                "Second status duplicate resource must be configurable");
        check(RuneEconomyConfig.duplicateCompensation(
                        secondStatus, RuneInventory.NORMAL_RANK, 3) == 21,
                "Second status duplicate compensation must be isolated");

        System.setProperty("customguimod.runes.frost.awakening.resource", "rune_cores");
        System.setProperty("customguimod.runes.frost.awakening.type", "BY_RANK");
        System.setProperty("customguimod.runes.frost.awakening.rank.1", "4");
        System.setProperty("customguimod.runes.frost.awakening.rank.2", "9");
        check(ResourceManager.RUNE_CORES.equals(
                        RuneEconomyConfig.awakeningResourceForStatus(secondStatus)),
                "Second status awakening resource must be independent");
        check(RuneEconomyConfig.awakeningCost(
                        secondStatus, RuneInventory.NORMAL_RANK) == 4,
                "Second status ordinary awakening cost must be configurable");
        check(RuneEconomyConfig.awakeningCost(
                        secondStatus, RuneInventory.IMPROVED_RANK) == 9,
                "Second status improved awakening cost must be configurable");
        check(RuneEconomyConfig.isAwakeningPaymentConfigured(
                        secondStatus, RuneInventory.NORMAL_RANK),
                "Second status paid awakening must become active only after configuration");

        check(RuneEconomyConfig.purchaseUnitCost(RuneEconomyConfig.STATUS_POISON) == 100,
                "Second status purchase config must not modify Poison");
        check(ResourceManager.RUNE_DUST.equals(
                        RuneEconomyConfig.awakeningResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Second status awakening config must not modify Poison");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 100,
                "Poison technical awakening baseline must remain unchanged");

        clearProperties();
    }

    private static void clearProperties() {
        String[] keys = {
                "customguimod.runes.status.2.key",
                "customguimod.runes.frost.purchase.resource",
                "customguimod.runes.frost.purchase.cost",
                "customguimod.runes.frost.purchase.event.multiplier",
                "customguimod.runes.frost.duplicate.resource",
                "customguimod.runes.frost.duplicate.type",
                "customguimod.runes.frost.duplicate.fixed",
                "customguimod.runes.frost.duplicate.rank.1",
                "customguimod.runes.frost.duplicate.rank.2",
                "customguimod.runes.frost.duplicate.formula.base",
                "customguimod.runes.frost.duplicate.formula.per_rank",
                "customguimod.runes.frost.event.multiplier",
                "customguimod.runes.frost.awakening.resource",
                "customguimod.runes.frost.awakening.type",
                "customguimod.runes.frost.awakening.fixed",
                "customguimod.runes.frost.awakening.rank.1",
                "customguimod.runes.frost.awakening.rank.2",
                "customguimod.runes.frost.awakening.rank.3",
                "customguimod.runes.frost.awakening.formula.base",
                "customguimod.runes.frost.awakening.formula.per_rank",
                "customguimod.runes.frost.awakening.event.multiplier"
        };
        for (String key : keys) System.clearProperty(key);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
