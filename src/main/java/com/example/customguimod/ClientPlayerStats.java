package com.example.customguimod;

public final class ClientPlayerStats {

    private static int coins;
    private static int crystals;
    private static int runeResources;
    private static int totalDamage;

    private ClientPlayerStats() {}

    public static void update(int newCoins, int newCrystals, int newRuneResources, int newTotalDamage) {
        coins = newCoins;
        crystals = newCrystals;
        runeResources = newRuneResources;
        totalDamage = newTotalDamage;
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

    public static int getRuneResources() {
        return runeResources;
    }

    public static int getTotalDamage() {
        return totalDamage;
    }
}
