package com.example.customguimod;

/** Central economy rules for rune statuses. Gameplay code should read values here instead of hard-coding them. */
final class RuneEconomyConfig {
    static final int STATUS_POISON = 1;

    enum CalculationType {
        FIXED,
        BY_RANK,
        FORMULA
    }

    private RuneEconomyConfig() {}

    static String resourceForStatus(int statusId) {
        if (statusId == STATUS_POISON) return ResourceManager.RUNE_DUST;
        return "";
    }

    static String purchaseResourceForStatus(int statusId) {
        return actionResourceForStatus(statusId, "purchase", "coins");
    }

    static int purchaseUnitCost(int statusId) {
        String key = statusKey(statusId);
        int baseCost = intProperty(
                "customguimod.runes." + key + ".purchase.cost",
                purchaseCostDefault(statusId)
        );

        double eventMultiplier = actionEventMultiplier(statusId, "purchase");
        long adjusted = Math.round(Math.max(1, baseCost) * Math.max(0.0D, eventMultiplier));
        if (adjusted <= 0L) return 1;
        return adjusted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) adjusted;
    }

    static int purchaseCost(int statusId, int amount) {
        if (amount <= 0) return 0;
        long total = (long) purchaseUnitCost(statusId) * amount;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    /**
     * v1.2.6 baseline: Poison awakening is paid with rune dust by default.
     * Values remain configurable and are not a final economy balance.
     */
    static String awakeningResourceForStatus(int statusId) {
        return actionResourceForStatus(
                statusId,
                "awakening",
                statusId == STATUS_POISON ? ResourceManager.RUNE_DUST : ""
        );
    }

    static CalculationType awakeningCalculationType(int statusId) {
        return actionCalculationType(statusId, "awakening");
    }

    static int awakeningCost(int statusId, int rank) {
        return actionRankCost(statusId, "awakening", rank, awakeningCostDefault(statusId));
    }

    static boolean isAwakeningPaymentConfigured(int statusId, int rank) {
        return !awakeningResourceForStatus(statusId).isEmpty() && awakeningCost(statusId, rank) > 0;
    }

    /**
     * Prepared for future rune upgrades. A zero default keeps current gameplay unchanged.
     */
    static String upgradeResourceForStatus(int statusId) {
        return actionResourceForStatus(statusId, "upgrade", "");
    }

    static CalculationType upgradeCalculationType(int statusId) {
        return actionCalculationType(statusId, "upgrade");
    }

    static int upgradeCost(int statusId, int rank) {
        return actionRankCost(statusId, "upgrade", rank, 0);
    }

    static boolean isUpgradePaymentConfigured(int statusId, int rank) {
        return !upgradeResourceForStatus(statusId).isEmpty() && upgradeCost(statusId, rank) > 0;
    }

    static CalculationType duplicateCalculationType(int statusId) {
        String key = statusKey(statusId);
        String raw = System.getProperty(
                "customguimod.runes." + key + ".duplicate.type",
                "FIXED"
        );
        try {
            return CalculationType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CalculationType.FIXED;
        }
    }

    static int duplicateCompensationPerRune(int statusId, int rank) {
        String key = statusKey(statusId);
        CalculationType type = duplicateCalculationType(statusId);
        int safeRank = Math.max(RuneInventory.NORMAL_RANK, rank);
        int amount;

        switch (type) {
            case BY_RANK:
                amount = intProperty(
                        "customguimod.runes." + key + ".duplicate.rank." + safeRank,
                        fixedDefault(statusId)
                );
                break;
            case FORMULA:
                int base = intProperty(
                        "customguimod.runes." + key + ".duplicate.formula.base",
                        fixedDefault(statusId)
                );
                int perRank = intProperty(
                        "customguimod.runes." + key + ".duplicate.formula.per_rank",
                        0
                );
                amount = base + Math.max(0, safeRank - RuneInventory.NORMAL_RANK) * perRank;
                break;
            case FIXED:
            default:
                amount = intProperty(
                        "customguimod.runes." + key + ".duplicate.fixed",
                        fixedDefault(statusId)
                );
                break;
        }

        double eventMultiplier = doubleProperty(
                "customguimod.runes.event.multiplier",
                1.0D
        ) * doubleProperty(
                "customguimod.runes." + key + ".event.multiplier",
                1.0D
        );

        return Math.max(0, (int) Math.round(amount * Math.max(0.0D, eventMultiplier)));
    }

    static int duplicateCompensation(int statusId, int rank, int duplicateCount) {
        if (duplicateCount <= 0) return 0;
        long total = (long) duplicateCompensationPerRune(statusId, rank) * duplicateCount;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private static String actionResourceForStatus(int statusId, String action, String fallback) {
        String key = statusKey(statusId);
        String raw = System.getProperty(
                "customguimod.runes." + key + "." + action + ".resource",
                fallback
        );
        String normalized = ResourceManager.normalize(raw).trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static CalculationType actionCalculationType(int statusId, String action) {
        String key = statusKey(statusId);
        String raw = System.getProperty(
                "customguimod.runes." + key + "." + action + ".type",
                "FIXED"
        );
        try {
            return CalculationType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CalculationType.FIXED;
        }
    }

    private static int actionRankCost(int statusId, String action, int rank, int fallback) {
        String key = statusKey(statusId);
        int safeRank = Math.max(RuneInventory.NORMAL_RANK, rank);
        CalculationType type = actionCalculationType(statusId, action);
        int amount;

        switch (type) {
            case BY_RANK:
                amount = intProperty(
                        "customguimod.runes." + key + "." + action + ".rank." + safeRank,
                        fallback
                );
                break;
            case FORMULA:
                int base = intProperty(
                        "customguimod.runes." + key + "." + action + ".formula.base",
                        fallback
                );
                int perRank = intProperty(
                        "customguimod.runes." + key + "." + action + ".formula.per_rank",
                        0
                );
                amount = base + Math.max(0, safeRank - RuneInventory.NORMAL_RANK) * perRank;
                break;
            case FIXED:
            default:
                amount = intProperty(
                        "customguimod.runes." + key + "." + action + ".fixed",
                        fallback
                );
                break;
        }

        double multiplier = actionEventMultiplier(statusId, action);
        long adjusted = Math.round(Math.max(0, amount) * Math.max(0.0D, multiplier));
        if (adjusted <= 0L) return 0;
        return adjusted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) adjusted;
    }

    private static double actionEventMultiplier(int statusId, String action) {
        String key = statusKey(statusId);
        return doubleProperty(
                "customguimod.runes." + action + ".event.multiplier",
                1.0D
        ) * doubleProperty(
                "customguimod.runes." + key + "." + action + ".event.multiplier",
                1.0D
        );
    }

    private static int fixedDefault(int statusId) {
        if (statusId == STATUS_POISON) return 10;
        return 0;
    }

    private static int purchaseCostDefault(int statusId) {
        if (statusId == STATUS_POISON) return 100;
        return 100;
    }

    private static int awakeningCostDefault(int statusId) {
        if (statusId == STATUS_POISON) return 100;
        return 0;
    }

    private static String statusKey(int statusId) {
        if (statusId == STATUS_POISON) return "poison";
        return "status_" + statusId;
    }

    private static int intProperty(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double doubleProperty(String key, double fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
