package com.example.customguimod;

public final class ClientPlayerStats {

    private static long coins;
    private static long crystals;
    private static long runeDust;
    private static long runeShards;
    private static long runeCores;
    private static int totalDamage;
    private static int activeRuneStatusId = RuneStatusRegistry.STATUS_POISON;
    private static String purchaseResource = "coins";
    private static int purchaseUnitCost = 100;
    private static String awakeningResource = "";
    private static int awakeningNormalCost;
    private static int awakeningImprovedCost;

    private ClientPlayerStats() {}

    public static void update(long newCoins, long newCrystals, long newRuneDust,
                              long newRuneShards, long newRuneCores, int newTotalDamage,
                              int newActiveRuneStatusId,
                              String newPurchaseResource, int newPurchaseUnitCost,
                              String newAwakeningResource, int newAwakeningNormalCost,
                              int newAwakeningImprovedCost) {
        coins = newCoins;
        crystals = newCrystals;
        runeDust = newRuneDust;
        runeShards = newRuneShards;
        runeCores = newRuneCores;
        totalDamage = newTotalDamage;
        activeRuneStatusId = newActiveRuneStatusId;
        purchaseResource = newPurchaseResource == null || newPurchaseResource.trim().isEmpty()
                ? "coins" : ResourceManager.normalize(newPurchaseResource);
        purchaseUnitCost = Math.max(1, newPurchaseUnitCost);
        awakeningResource = newAwakeningResource == null
                ? "" : ResourceManager.normalize(newAwakeningResource);
        awakeningNormalCost = Math.max(0, newAwakeningNormalCost);
        awakeningImprovedCost = Math.max(0, newAwakeningImprovedCost);
    }

    public static void setCoins(long newCoins) { coins = newCoins; }
    public static long getCoins() { return coins; }
    public static long getCrystals() { return crystals; }
    public static long getRuneDust() { return runeDust; }
    public static long getRuneShards() { return runeShards; }
    public static long getRuneCores() { return runeCores; }
    public static int getTotalDamage() { return totalDamage; }
    public static int getActiveRuneStatusId() { return activeRuneStatusId; }
    public static String getPurchaseResource() { return purchaseResource; }
    public static int getPurchaseUnitCost() { return purchaseUnitCost; }
    public static String getAwakeningResource() { return awakeningResource; }
    public static int getAwakeningNormalCost() { return awakeningNormalCost; }
    public static int getAwakeningImprovedCost() { return awakeningImprovedCost; }

    public static int getAwakeningCostForRank(int rank) {
        if (rank >= RuneInventory.IMPROVED_RANK) return awakeningImprovedCost;
        return awakeningNormalCost;
    }

    public static long getResourceBalance(String resource) {
        String normalized = ResourceManager.normalize(resource);
        if ("coins".equals(normalized)) return coins;
        if ("crystals".equals(normalized)) return crystals;
        if (ResourceManager.RUNE_DUST.equals(normalized)) return runeDust;
        if (ResourceManager.RUNE_SHARDS.equals(normalized)) return runeShards;
        if (ResourceManager.RUNE_CORES.equals(normalized)) return runeCores;
        return -1L;
    }
}
