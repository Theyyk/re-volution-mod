package com.example.customguimod;

/** Exercises the fixed 26-rune catalog and rank states without Minecraft or MongoDB. */
public final class RuneInventoryTest {
    public static void main(String[] args) {
        PrimaryRuneCatalogTest.run();
        RuneEconomyConfigTest.run();
        RuneProgressionTest.run();

        RuneInventory inventory = new RuneInventory();
        inventory.set(0, 0, 1);
        inventory.set(1, 1, 2);
        inventory.set(26, 1, 3);

        check(inventory.occupiedCount() == 3, "Legacy records must still load without being dropped");
        check(inventory.occupiedTypeCount() == 2, "Duplicate records must represent one rune type");
        check(inventory.getBoost(1).layer == RuneInventory.ADVANCED_RANK,
                "The visible rune must use the highest saved rank");
        check(inventory.ownedCatalogSlotCount() == 2,
                "Collection progress must count unique rune types, not purchases");
        check(!inventory.hasAllCatalogRunes(), "Two owned runes are not a complete collection");
        check(!inventory.isPurchaseLimitReached(), "Purchases must remain available before 26 / 26");
        check(!inventory.canAwaken(1), "Ranks stay locked until all 26 runes are collected");
        check(inventory.awakeningDecision(0).reason == RuneAwakeningDecision.Reason.COLLECTION_BEHIND,
                "Inventory must surface collection-behind awakening state");

        inventory.clear();
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            inventory.set(rune, rune, RuneInventory.NORMAL_RANK);
        }

        check(inventory.occupiedCount() == 26, "The first element must keep exactly 26 fixed stored positions");
        check(inventory.ownedCatalogSlotCount() == 26, "All 26 rune types must complete the collection");
        check(inventory.hasAllCatalogRunes(), "A full collection must unlock rank testing");
        check(inventory.isPurchaseLimitReached(),
                "A complete collection must block purchases and move the player to awakening");
        check(inventory.canAwaken(0), "An ordinary rune may become improved after collection completion");
        check(inventory.awakeningDecision(0).nextRank == RuneInventory.IMPROVED_RANK,
                "Inventory decision must target improved rank from ordinary");

        inventory.set(0, 0, RuneInventory.IMPROVED_RANK);
        check(inventory.getBoost(0).layer == RuneInventory.IMPROVED_RANK,
                "Improved rank must be stored independently for its fixed rune");
        check(!inventory.canAwaken(0),
                "An improved rune must wait until all runes have reached improved rank");
        check(inventory.awakeningDecision(0).reason == RuneAwakeningDecision.Reason.COLLECTION_BEHIND,
                "Inventory must expose why an improved rune is blocked");

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            inventory.set(rune, rune, RuneInventory.IMPROVED_RANK);
        }
        check(inventory.canAwaken(0), "An improved rune may become advanced once the collection catches up");
        check(inventory.awakeningDecision(0).nextRank == RuneInventory.ADVANCED_RANK,
                "Inventory decision must target advanced rank from improved");

        inventory.set(0, 0, RuneInventory.ADVANCED_RANK);
        check(inventory.getBoost(0).layer == RuneInventory.ADVANCED_RANK,
                "Advanced rank must be the highest visible state");
        check(!inventory.canAwaken(0), "Advanced is the current maximum rank");
        check(inventory.awakeningDecision(0).reason == RuneAwakeningDecision.Reason.MAXIMUM_RANK,
                "Inventory must expose maximum-rank awakening state");

        inventory.set(1, 1, 99);
        check(inventory.getBoost(1).layer == RuneInventory.ADVANCED_RANK,
                "Incoming ranks above advanced must be clamped to the maximum");
        inventory.set(2, 2, -10);
        check(inventory.getBoost(2).layer == RuneInventory.NORMAL_RANK,
                "Incoming ranks below normal must be clamped to normal");

        inventory.set(-1, 4, 1);
        inventory.set(50, 4, 1);
        inventory.set(4, -1, 1);
        inventory.set(4, 26, 1);
        check(inventory.occupiedCount() == 26,
                "Invalid storage slots and rune indexes must be ignored");

        java.util.List<Integer> occupied = new java.util.ArrayList<>();
        for (int slot = 0; slot < 25; slot++) occupied.add(slot);
        java.util.List<Integer> free = RuneInventory.freePurchaseSlots(occupied);
        check(free.size() == 1 && free.get(0) == 25,
                "Legacy fixed-slot helper must still expose the missing 26th position");

        System.out.println("26 fixed rune slots, awakening progression, rune economy and rank regression checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
