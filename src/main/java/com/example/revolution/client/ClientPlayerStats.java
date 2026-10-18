package com.example.revolution.client;

import com.example.revolution.rune.RuneInventory;
import com.example.revolution.rune.RuneStatusRegistry;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.resource.ResourceManager;

import java.math.BigDecimal;

public final class ClientPlayerStats {
    private static BigDecimal coins = BigDecimal.ZERO;
    private static BigDecimal crystals = BigDecimal.ZERO;
    private static BigDecimal runeDust = BigDecimal.ZERO;
    private static BigDecimal runeShards = BigDecimal.ZERO;
    private static BigDecimal runeCores = BigDecimal.ZERO;
    private static int totalDamage;
    private static double resolvedClickDamage;
    private static double resolvedPoisonDamage;
    private static int activeRuneStatusId = RuneStatusRegistry.STATUS_POISON;
    private static String purchaseResource = "coins";
    private static int purchaseUnitCost = 100;
    private static String awakeningResource = "";
    private static int awakeningNormalCost;
    private static int awakeningImprovedCost;

    private ClientPlayerStats() {}

    public static void update(BigDecimal newCoins, BigDecimal newCrystals, BigDecimal newRuneDust,
                              BigDecimal newRuneShards, BigDecimal newRuneCores, int newTotalDamage,
                              int newActiveRuneStatusId, String newPurchaseResource, int newPurchaseUnitCost,
                              String newAwakeningResource, int newAwakeningNormalCost, int newAwakeningImprovedCost) {
        coins = safe(newCoins); crystals = safe(newCrystals); runeDust = safe(newRuneDust); runeShards = safe(newRuneShards); runeCores = safe(newRuneCores);
        totalDamage = newTotalDamage; activeRuneStatusId = newActiveRuneStatusId;
        purchaseResource = newPurchaseResource == null || newPurchaseResource.trim().isEmpty() ? "coins" : ResourceManager.normalize(newPurchaseResource);
        purchaseUnitCost = Math.max(1, newPurchaseUnitCost);
        awakeningResource = newAwakeningResource == null ? "" : ResourceManager.normalize(newAwakeningResource);
        awakeningNormalCost = Math.max(0, newAwakeningNormalCost); awakeningImprovedCost = Math.max(0, newAwakeningImprovedCost);
    }

    public static void setResolvedCombat(double clickDamage, double poisonDamage) {
        resolvedClickDamage = finiteNonNegative(clickDamage);
        resolvedPoisonDamage = finiteNonNegative(poisonDamage);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static BigDecimal safe(BigDecimal value) { return value == null || value.signum() < 0 ? BigDecimal.ZERO : LargeNumbers.normalize(value); }

    public static void setCoins(long newCoins) { coins = BigDecimal.valueOf(Math.max(0L, newCoins)); }

    /**
     * Applies the legacy int-sized coin synchronization only while the modern client value still
     * fits that transport. Large-number stats are authoritative once they exceed the legacy range.
     */
    public static void acceptLegacyCoins(int newCoins) {
        if (coins.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) return;
        setCoins(newCoins);
    }

    public static BigDecimal getCoins() { return coins; }
    public static BigDecimal getCrystals() { return crystals; }
    public static BigDecimal getRuneDust() { return runeDust; }
    public static BigDecimal getRuneShards() { return runeShards; }
    public static BigDecimal getRuneCores() { return runeCores; }
    public static int getTotalDamage() { return totalDamage; }
    public static double getResolvedClickDamage() { return resolvedClickDamage; }
    public static double getResolvedPoisonDamage() { return resolvedPoisonDamage; }
    public static int getActiveRuneStatusId() { return activeRuneStatusId; }
    public static String getPurchaseResource() { return purchaseResource; }
    public static int getPurchaseUnitCost() { return purchaseUnitCost; }
    public static String getAwakeningResource() { return awakeningResource; }
    public static int getAwakeningNormalCost() { return awakeningNormalCost; }
    public static int getAwakeningImprovedCost() { return awakeningImprovedCost; }

    public static int getAwakeningCostForRank(int rank) { return rank >= RuneInventory.IMPROVED_RANK ? awakeningImprovedCost : awakeningNormalCost; }

    public static BigDecimal getResourceBalance(String resource) {
        String normalized = ResourceManager.normalize(resource);
        if ("coins".equals(normalized)) return coins;
        if ("crystals".equals(normalized)) return crystals;
        if (ResourceManager.RUNE_DUST.equals(normalized)) return runeDust;
        if (ResourceManager.RUNE_SHARDS.equals(normalized)) return runeShards;
        if (ResourceManager.RUNE_CORES.equals(normalized)) return runeCores;
        return BigDecimal.valueOf(-1L);
    }
}
