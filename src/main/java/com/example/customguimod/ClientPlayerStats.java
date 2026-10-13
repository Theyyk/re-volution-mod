package com.example.customguimod;

public final class ClientPlayerStats {

    private static int coins;
    private static int crystals;
    private static int runeDust;
    private static int runeShards;
    private static int runeCores;
    private static int totalDamage;
    private static String purchaseResource = "coins";
    private static int purchaseUnitCost = 100;
    private static String awakeningResource = "";
    private static int awakeningNormalCost;
    private static int awakeningImprovedCost;

    private ClientPlayerStats() {}

    public static void update(int newCoins, int newCrystals, int newRuneDust,
                              int newRuneShards, int newRuneCores, int newTotalDamage,
                              String newPurchaseResource, int newPurchaseUnitCost,
                              String newAwakeningResource, int newAwakeningNormalCost,
                              int newAwakeningImprovedCost) {
        coins = newCoins;
        crystals = newCrystals;
        runeDust = newRuneDust;
        runeShards = newRuneShards;
        runeCores = newRuneCores;
        totalDamage = newTotalDamage;
        purchaseResource = newPurchaseResource == null || newPurchaseResource.trim().isEmpty()
                ? "coins" : ResourceManager.normalize(newPurchaseResource);
        purchaseUnitCost = Math.max(1, newPurchaseUnitCost);
        awakeningResource = newAwakeningResource == null
                ? "" : ResourceManager.normalize(newAwakeningResource);
        awakeningNormalCost = Math.max(0, newAwakeningNormalCost);
        awakeningImprovedCost = Math.max(0, newAwakeningImprovedCost);
    }

    public static void setCoins(int newCoins) { coins = newCoins; }
    public static int getCoins() { return coins; }
    public static int getCrystals() { return crystals; }
    public static int getRuneDust() { return runeDust; }
    public static int getRuneShards() { return runeShards; }
    public static int getRuneCores() { return runeCores; }
    public static int getTotalDamage() { return totalDamage; }
    public static String getPurchaseResource() { return purchaseResource; }
    public static int getPurchaseUnitCost() { return purchaseUnitCost; }
    public static String getAwakeningResource() { return awakeningResource; }
    public static int getAwakeningNormalCost() { return awakeningNormalCost; }
    public static int getAwakeningImprovedCost() { return awakeningImprovedCost; }

    public static int getAwakeningCostForRank(int rank) {
        if (rank >= RuneInventory.IMPROVED_RANK) return awakeningImprovedCost;
        return awakeningNormalCost;
    }

    public static int getResourceBalance(String resource) {
        String normalized = ResourceManager.normalize(resource);
        if ("coins".equals(normalized)) return coins;
        if ("crystals".equals(normalized)) return crystals;
        if (ResourceManager.RUNE_DUST.equals(normalized)) return runeDust;
        if (ResourceManager.RUNE_SHARDS.equals(normalized)) return runeShards;
        if (ResourceManager.RUNE_CORES.equals(normalized)) return runeCores;
        return -1;
    }
}
