package com.example.customguimod;

/** Pure Java regression checks for configurable rune economy rules. */
public final class RuneEconomyConfigTest {
    public static void main(String[] args) {
        run();
        System.out.println("Rune economy purchase, paid awakening, future upgrade, duplicate and event modifier checks passed");
    }

    static void run() {
        clearProperties();

        check("coins".equals(
                        RuneEconomyConfig.purchaseResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Poison rune purchases must default to coins");
        check(RuneEconomyConfig.purchaseUnitCost(RuneEconomyConfig.STATUS_POISON) == 100,
                "Poison rune purchases must default to 100 coins");
        check(RuneEconomyConfig.purchaseCost(RuneEconomyConfig.STATUS_POISON, 5) == 500,
                "Five poison rune attempts must cost 500 coins by default");

        System.setProperty("customguimod.runes.poison.purchase.cost", "125");
        check(RuneEconomyConfig.purchaseCost(RuneEconomyConfig.STATUS_POISON, 2) == 250,
                "Purchase cost must be adjustable outside purchase logic");

        System.setProperty("customguimod.runes.poison.purchase.resource", "crystals");
        check("crystals".equals(
                        RuneEconomyConfig.purchaseResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Purchase resource must be configurable per status");

        System.setProperty("customguimod.runes.purchase.event.multiplier", "0.5");
        check(RuneEconomyConfig.purchaseUnitCost(RuneEconomyConfig.STATUS_POISON) == 63,
                "Purchase event multiplier must modify the status purchase cost");

        clearProperties();

        check(ResourceManager.RUNE_DUST.equals(
                        RuneEconomyConfig.awakeningResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Poison awakening must default to rune dust in v1.2.6");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 100,
                "Poison awakening must have a technical 100 rune dust baseline");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 100,
                "Fixed awakening baseline must apply to improved runes too");
        check(RuneEconomyConfig.isAwakeningPaymentConfigured(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK),
                "Paid awakening must be enabled by default for Poison in v1.2.6");

        System.setProperty("customguimod.runes.poison.awakening.type", "BY_RANK");
        System.setProperty("customguimod.runes.poison.awakening.rank.1", "40");
        System.setProperty("customguimod.runes.poison.awakening.rank.2", "80");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 40,
                "Awakening BY_RANK cost must support current-rank pricing");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 80,
                "Awakening BY_RANK cost must support improved-rank pricing");

        System.setProperty("customguimod.runes.awakening.event.multiplier", "0.5");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 40,
                "Awakening event multiplier must modify the configured rank cost");

        clearProperties();
        System.setProperty("customguimod.runes.poison.awakening.type", "FORMULA");
        System.setProperty("customguimod.runes.poison.awakening.formula.base", "60");
        System.setProperty("customguimod.runes.poison.awakening.formula.per_rank", "25");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 60,
                "Awakening FORMULA must use configured base cost");
        check(RuneEconomyConfig.awakeningCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 85,
                "Awakening FORMULA must support rank growth");

        clearProperties();

        check(RuneEconomyConfig.upgradeResourceForStatus(RuneEconomyConfig.STATUS_POISON).isEmpty(),
                "Upgrade resource must remain unset until upgrade economy is enabled");
        check(RuneEconomyConfig.upgradeCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 0,
                "Upgrade economy must remain inactive by default");

        System.setProperty("customguimod.runes.poison.upgrade.resource", "rune_shards");
        System.setProperty("customguimod.runes.poison.upgrade.type", "FORMULA");
        System.setProperty("customguimod.runes.poison.upgrade.formula.base", "25");
        System.setProperty("customguimod.runes.poison.upgrade.formula.per_rank", "10");
        check(ResourceManager.RUNE_SHARDS.equals(
                        RuneEconomyConfig.upgradeResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Upgrade resource must be configurable per status");
        check(RuneEconomyConfig.upgradeCost(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 35,
                "Upgrade FORMULA cost must support rank-based growth");
        check(RuneEconomyConfig.isUpgradePaymentConfigured(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK),
                "Upgrade payment must become configured when resource and positive cost are provided");

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
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.IMPROVED_RANK) == 15,
                "Formula mode must support rank-based growth");

        System.setProperty("customguimod.runes.event.multiplier", "2.0");
        check(RuneEconomyConfig.duplicateCompensationPerRune(
                        RuneEconomyConfig.STATUS_POISON, RuneInventory.NORMAL_RANK) == 20,
                "Event multiplier must modify the base duplicate calculation without replacing it");

        clearProperties();
    }

    private static void clearProperties() {
        String[] keys = {
                "customguimod.runes.poison.purchase.resource",
                "customguimod.runes.poison.purchase.cost",
                "customguimod.runes.purchase.event.multiplier",
                "customguimod.runes.poison.purchase.event.multiplier",

                "customguimod.runes.poison.awakening.resource",
                "customguimod.runes.poison.awakening.type",
                "customguimod.runes.poison.awakening.fixed",
                "customguimod.runes.poison.awakening.rank.1",
                "customguimod.runes.poison.awakening.rank.2",
                "customguimod.runes.poison.awakening.rank.3",
                "customguimod.runes.poison.awakening.formula.base",
                "customguimod.runes.poison.awakening.formula.per_rank",
                "customguimod.runes.awakening.event.multiplier",
                "customguimod.runes.poison.awakening.event.multiplier",

                "customguimod.runes.poison.upgrade.resource",
                "customguimod.runes.poison.upgrade.type",
                "customguimod.runes.poison.upgrade.fixed",
                "customguimod.runes.poison.upgrade.rank.1",
                "customguimod.runes.poison.upgrade.rank.2",
                "customguimod.runes.poison.upgrade.rank.3",
                "customguimod.runes.poison.upgrade.formula.base",
                "customguimod.runes.poison.upgrade.formula.per_rank",
                "customguimod.runes.upgrade.event.multiplier",
                "customguimod.runes.poison.upgrade.event.multiplier",

                "customguimod.runes.poison.duplicate.type",
                "customguimod.runes.poison.duplicate.fixed",
                "customguimod.runes.poison.duplicate.formula.base",
                "customguimod.runes.poison.duplicate.formula.per_rank",
                "customguimod.runes.poison.duplicate.rank.1",
                "customguimod.runes.poison.duplicate.rank.2",
                "customguimod.runes.poison.duplicate.rank.3",
                "customguimod.runes.event.multiplier",
                "customguimod.runes.poison.event.multiplier"
        };
        for (String key : keys) System.clearProperty(key);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
