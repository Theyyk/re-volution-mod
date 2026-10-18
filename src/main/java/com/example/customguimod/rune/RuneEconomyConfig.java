package com.example.customguimod.rune;
import com.example.customguimod.resource.ResourceManager;

/** Central economy rules for rune statuses. Gameplay code should read values here instead of hard-coding them. */
public final class RuneEconomyConfig {
    public static final int STATUS_POISON = RuneStatusRegistry.STATUS_POISON;

    public enum CalculationType {
        FIXED,
        BY_RANK,
        FORMULA
    }

    private RuneEconomyConfig() {}

    public static String resourceForStatus(int statusId) {
        String fallback = statusId == STATUS_POISON ? ResourceManager.RUNE_DUST : "";
        return actionResourceForStatus(statusId, "duplicate", fallback);
    }

    public static String purchaseResourceForStatus(int statusId) {
        return actionResourceForStatus(statusId, "purchase", "coins");
    }

    public static int purchaseUnitCost(int statusId) {
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

    public static int purchaseCost(int statusId, int amount) {
        if (amount <= 0) return 0;
        long total = (long) purchaseUnitCost(statusId) * amount;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    /**
     * v1.2.6 baseline: Poison awakening is paid with rune dust by default.
     * Other statuses are opt-in through status-specific configuration.
     */
    public static String awakeningResourceForStatus(int statusId) {
        return actionResourceForStatus(
                statusId,
                "awakening",
                statusId == STATUS_POISON ? ResourceManager.RUNE_DUST : ""
        );
    }

    public static CalculationType awakeningCalculationType(int statusId) {
        return actionCalculationType(statusId, "awakening");
    }

    public static int awakeningCost(int statusId, int rank) {
        return actionRankCost(statusId, "awakening", rank, awakeningCostDefault(statusId));
    }

    public static boolean isAwakeningPaymentConfigured(int statusId, int rank) {
        return !awakeningResourceForStatus(statusId).isEmpty() && awakeningCost(statusId, rank) > 0;
    }

    /**
     * v1.2.9 enhancement foundation. Enhancement belongs to the rune slot and has no maximum level.
     * Poison defaults to rune dust, while other statuses remain opt-in until their content is connected.
     */
    public static String enhancementResourceForStatus(int statusId) {
        return actionResourceForStatus(
                statusId,
                "enhancement",
                statusId == STATUS_POISON ? ResourceManager.RUNE_DUST : ""
        );
    }

    public static CalculationType enhancementCalculationType(int statusId) {
        return actionCalculationType(statusId, "enhancement");
    }

    /** Cost of the next single enhancement when the slot is currently at currentLevel. */
    public static int enhancementNextCost(int statusId, int currentLevel) {
        String key = statusKey(statusId);
        int safeLevel = Math.max(0, currentLevel);
        CalculationType type = enhancementCalculationType(statusId);
        int fallback = enhancementCostDefault(statusId);
        int amount;

        switch (type) {
            case FORMULA:
                int base = intProperty(
                        "customguimod.runes." + key + ".enhancement.formula.base",
                        fallback
                );
                int perLevel = intProperty(
                        "customguimod.runes." + key + ".enhancement.formula.per_level",
                        0
                );
                long formula = (long) base + (long) safeLevel * perLevel;
                amount = formula > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, formula);
                break;
            case BY_RANK:
            case FIXED:
            default:
                amount = intProperty(
                        "customguimod.runes." + key + ".enhancement.fixed",
                        fallback
                );
                break;
        }

        double multiplier = actionEventMultiplier(statusId, "enhancement");
        long adjusted = Math.round(Math.max(0, amount) * Math.max(0.0D, multiplier));
        if (adjusted <= 0L) return 0;
        return adjusted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) adjusted;
    }

    /**
     * Sequential bulk cost. Every requested future level is priced independently so a later growing
     * formula can be introduced without changing the server action or GUI contract.
     */
    public static int enhancementCost(int statusId, int currentLevel, int amount) {
        if (amount <= 0) return 0;
        long total = 0L;
        int safeLevel = Math.max(0, currentLevel);
        for (int i = 0; i < amount; i++) {
            total += enhancementNextCost(statusId, safeLevel + i);
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public static int affordableEnhancementAmount(int statusId, int currentLevel, int requestedAmount, int balance) {
        if (requestedAmount <= 0 || balance < 0) return 0;
        long total = 0L;
        int safeLevel = Math.max(0, currentLevel);
        int accepted = 0;
        for (int i = 0; i < requestedAmount; i++) {
            int next = enhancementNextCost(statusId, safeLevel + i);
            if (next <= 0) break;
            if (total + next > balance) break;
            total += next;
            accepted++;
        }
        return accepted;
    }

    public static double enhancementPowerStep(int statusId) {
        String key = statusKey(statusId);
        return Math.max(0.0D, doubleProperty(
                "customguimod.runes." + key + ".enhancement.power_step",
                0.1D
        ));
    }

    public static double enhancementPowerMultiplier(int statusId, int enhancementLevel) {
        return 1.0D + Math.max(0, enhancementLevel) * enhancementPowerStep(statusId);
    }

    /** Backward-compatible preparatory alias retained for older callers until v1.2.9 GUI migration is complete. */
    public static String upgradeResourceForStatus(int statusId) {
        return enhancementResourceForStatus(statusId);
    }

    public static CalculationType upgradeCalculationType(int statusId) {
        return enhancementCalculationType(statusId);
    }

    public static int upgradeCost(int statusId, int rank) {
        return enhancementNextCost(statusId, 0);
    }

    public static boolean isUpgradePaymentConfigured(int statusId, int rank) {
        return !enhancementResourceForStatus(statusId).isEmpty() && enhancementNextCost(statusId, 0) > 0;
    }

    public static CalculationType duplicateCalculationType(int statusId) {
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

    public static int duplicateCompensationPerRune(int statusId, int rank) {
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

    public static int duplicateCompensation(int statusId, int rank, int duplicateCount) {
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
        return 100;
    }

    private static int awakeningCostDefault(int statusId) {
        if (statusId == STATUS_POISON) return 100;
        return 0;
    }

    private static int enhancementCostDefault(int statusId) {
        if (statusId == STATUS_POISON) return 10;
        return 0;
    }

    private static String statusKey(int statusId) {
        return RuneStatusRegistry.statusKey(statusId);
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
