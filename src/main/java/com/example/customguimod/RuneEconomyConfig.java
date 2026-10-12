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

    private static int fixedDefault(int statusId) {
        if (statusId == STATUS_POISON) return 10;
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
