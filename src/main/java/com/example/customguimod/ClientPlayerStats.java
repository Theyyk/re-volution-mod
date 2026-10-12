package com.example.customguimod;

public final class ClientPlayerStats {

    private static int coins;
    private static int crystals;
    private static int runeDust;
    private static int runeShards;
    private static int runeCores;
    private static int totalDamage;

    private ClientPlayerStats() {}

    public static void update(int newCoins, int newCrystals, int newRuneDust,
                              int newRuneShards, int newRuneCores, int newTotalDamage) {
        coins = newCoins;
        crystals = newCrystals;
        runeDust = newRuneDust;
        runeShards = newRuneShards;
        runeCores = newRuneCores;
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
}
