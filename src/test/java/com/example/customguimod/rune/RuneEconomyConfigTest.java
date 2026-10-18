package com.example.customguimod.rune;
import com.example.customguimod.resource.ResourceManager;

/** Pure Java regression checks for configurable rune economy rules. */
public final class RuneEconomyConfigTest {
    public static void main(String[] args) {
        run();
        System.out.println("Rune economy purchase, awakening, enhancement, duplicate and event modifier checks passed");
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

        check(ResourceManager.RUNE_DUST.equals(
                        RuneEconomyConfig.enhancementResourceForStatus(RuneEconomyConfig.STATUS_POISON)),
                "Poison enhancement must default to rune dust in v1.2.9");
        check(RuneEconomyConfig.enhancementNextCost(RuneEconomyConfig.STATUS_POISON, 0) == 10,
                "One poison enhancement must default to 10 rune dust");
        check(RuneEconomyConfig.enhancementCost(RuneEconomyConfig.STATUS_POISON, 0, 100) == 1000,
                "Flat v1.2.9 baseline must price x100 as 100 sequential ten-dust levels");
        check(RuneEconomyConfig.affordableEnhancementAmount(
                        RuneEconomyConfig.STATUS_POISON, 0, 100, 55) == 5,
                "Bulk enhancement must stop at the affordable sequential level count");
        check(Math.abs(RuneEconomyConfig.enhancementPowerMultiplier(
                        RuneEconomyConfig.STATUS_POISON, 1) - 1.1D) < 0.000001D,
                "+1 enhancement must default to x1.1 power");
        check(Math.abs(RuneEconomyConfig.enhancementPowerMultiplier(
                        RuneEconomyConfig.STATUS_POISON, 10) - 2.0D) < 0.000001D,
                "+10 enhancement must default to x2.0 power");

        System.setProperty("customguimod.runes.poison.enhancement.type", "FORMULA");
        System.setProperty("customguimod.runes.poison.enhancement.formula.base", "10");
        System.setProperty("customguimod.runes.poison.enhancement.formula.per_level", "2");
        check(RuneEconomyConfig.enhancementNextCost(RuneEconomyConfig.STATUS_POISON, 5) == 20,
                "Enhancement FORMULA must be able to grow with current slot level");
        check(RuneEconomyConfig.enhancementCost(RuneEconomyConfig.STATUS_POISON, 5, 3) == 66,
                "Bulk enhancement must sum each future level independently: 20 + 22 + 24");
        check(RuneEconomyConfig.affordableEnhancementAmount(
                        RuneEconomyConfig.STATUS_POISON, 5, 100, 43) == 2,
                "Affordable bulk count must use the sequential growing formula");

        System.setProperty("customguimod.runes.poison.enhancement.power_step", "0.25");
        check(Math.abs(RuneEconomyConfig.enhancementPowerMultiplier(
                        RuneEconomyConfig.STATUS_POISON, 4) - 2.0D) < 0.000001D,
                "Enhancement power step must be configurable without changing stored levels");

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

                "customguimod.runes.poison.enhancement.resource",
                "customguimod.runes.poison.enhancement.type",
                "customguimod.runes.poison.enhancement.fixed",
                "customguimod.runes.poison.enhancement.formula.base",
                "customguimod.runes.poison.enhancement.formula.per_level",
                "customguimod.runes.enhancement.event.multiplier",
                "customguimod.runes.poison.enhancement.event.multiplier",
                "customguimod.runes.poison.enhancement.power_step",

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
