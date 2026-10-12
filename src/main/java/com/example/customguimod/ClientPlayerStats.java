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

    private ClientPlayerStats() {}

    public static void update(int newCoins, int newCrystals, int newRuneDust,
                              int newRuneShards, int newRuneCores, int newTotalDamage,
                              String newPurchaseResource, int newPurchaseUnitCost) {
        coins = newCoins;
        crystals = newCrystals;
        runeDust = newRuneDust;
        runeShards = newRuneShards;
        runeCores = newRuneCores;
        totalDamage = newTotalDamage;
        purchaseResource = newPurchaseResource == null || newPurchaseResource.trim().isEmpty()
                ? "coins" : ResourceManager.normalize(newPurchaseResource);
        purchaseUnitCost = Math.max(1, newPurchaseUnitCost);
    }

    public static void setCoins(int newCoins) {
        coins = newCoins;
    }

    public static int getCoins() {
        return coins;
    }

    public static int getCrystals() {
        return crystals;
    }

    public static int getRuneDust() {
        return runeDust;
    }

    public static int getRuneShards() {
        return runeShards;
    }

    public static int getRuneCores() {
        return runeCores;
    }

    public static int getTotalDamage() {
        return totalDamage;
    }

    public static String getPurchaseResource() {
        return purchaseResource;
    }

    public static int getPurchaseUnitCost() {
        return purchaseUnitCost;
    }
}
