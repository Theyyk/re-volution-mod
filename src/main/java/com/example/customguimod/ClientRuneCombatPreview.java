package com.example.customguimod;

import java.util.HashMap;
import java.util.Map;

/** Client-side preview of stable rune combat stats used by the GUI. */
final class ClientRuneCombatPreview {
    private ClientRuneCombatPreview() {}

    static double clickDamage(double vanillaClickDamage) {
        int[] ranks = clientRanks();
        int[] enhancementLevels = clientEnhancementLevels();
        return clickDamage(
                ClientPlayerStats.getActiveRuneStatusId(),
                vanillaClickDamage,
                ranks,
                enhancementLevels,
                ClientRuneEnhancements.getPowerStep()
        );
    }

    static double poisonDamage() {
        int[] ranks = clientRanks();
        int[] enhancementLevels = clientEnhancementLevels();
        return poisonDamage(
                ClientPlayerStats.getActiveRuneStatusId(),
                ranks,
                enhancementLevels,
                ClientRuneEnhancements.getPowerStep()
        );
    }

    static double clickDamage(int statusId, double vanillaClickDamage, int[] ranks,
                              int[] enhancementLevels, double enhancementStep) {
        RuneCombatMath.Snapshot snapshot = snapshot(statusId, ranks, enhancementLevels, enhancementStep);
        if (snapshot == null) return Math.max(0.0D, Double.isFinite(vanillaClickDamage) ? vanillaClickDamage : 0.0D);
        // Statistics show the stable normal click. Random proc and temporary rage are intentionally excluded.
        return RuneCombatMath.clickDamage(vanillaClickDamage, snapshot, false);
    }

    static double poisonDamage(int statusId, int[] ranks, int[] enhancementLevels, double enhancementStep) {
        RuneCombatMath.Snapshot snapshot = snapshot(statusId, ranks, enhancementLevels, enhancementStep);
        return snapshot == null ? 0.0D : RuneCombatMath.poisonDamage(snapshot);
    }

    private static RuneCombatMath.Snapshot snapshot(int statusId, int[] ranks,
                                                    int[] enhancementLevels, double enhancementStep) {
        if (statusId != RuneStatusRegistry.STATUS_POISON) return null;

        double safeStep = Double.isFinite(enhancementStep) ? Math.max(0.0D, enhancementStep) : 0.0D;
        Map<String, Double> targetedBasePower = new HashMap<>();

        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = rankAt(ranks, rune);
            if (rank <= 0) continue;
            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
            if (definition == null) continue;

            double enhancementMultiplier = enhancementMultiplier(enhancementLevels, rune, safeStep);
            for (RuneTargetedPower power : definition.targetedPowers) {
                if (power == null) continue;
                double resolved = power.resolve(rank, enhancementMultiplier);
                if (!Double.isFinite(resolved)) continue;
                targetedBasePower.merge(power.targetKey, Math.max(0.0D, resolved), Double::sum);
            }
        }

        RuneCombatMath.Snapshot snapshot = new RuneCombatMath.Snapshot();
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            int rank = rankAt(ranks, rune);
            if (rank <= 0) continue;
            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(rune);
            if (definition == null) continue;

            double enhancementMultiplier = enhancementMultiplier(enhancementLevels, rune, safeStep);
            double basePowerBonus = targetedBasePower.getOrDefault(targetKeyForSlot(rune), 0.0D);

            for (RuneStat stat : definition.stats) {
                if (stat == null) continue;
                boolean targetedFlat = stat.type == RuneStatType.CLICK_DAMAGE_FLAT
                        || stat.type == RuneStatType.POISON_DAMAGE_FLAT;
                double value = stat.resolve(
                        rank,
                        enhancementMultiplier,
                        targetedFlat ? basePowerBonus : 0.0D
                ).finalValue;
                accumulate(snapshot, stat.type, value);
            }
        }
        return snapshot;
    }

    private static int[] clientRanks() {
        int[] ranks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (int rune = 0; rune < ranks.length; rune++) ranks[rune] = ClientRuneRanks.rankOf(rune);
        return ranks;
    }

    private static int[] clientEnhancementLevels() {
        int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (int rune = 0; rune < levels.length; rune++) levels[rune] = ClientRuneEnhancements.levelOf(rune);
        return levels;
    }

    private static int rankAt(int[] ranks, int rune) {
        if (ranks == null || rune < 0 || rune >= ranks.length) return 0;
        int rank = ranks[rune];
        return rank <= 0 ? 0 : RuneProgression.clampRank(rank);
    }

    private static double enhancementMultiplier(int[] levels, int rune, double step) {
        int level = levels != null && rune >= 0 && rune < levels.length ? Math.max(0, levels[rune]) : 0;
        double multiplier = 1.0D + level * step;
        return Double.isFinite(multiplier) ? Math.max(1.0D, multiplier) : 1.0D;
    }

    private static String targetKeyForSlot(int slot) {
        if (slot >= 0 && slot <= 3) return "killer_dagger";
        if (slot >= 13 && slot <= 16) return "snake_fang";
        return "";
    }

    private static void accumulate(RuneCombatMath.Snapshot snapshot, RuneStatType type, double value) {
        if (!Double.isFinite(value)) return;
        switch (type) {
            case CLICK_DAMAGE_FLAT:
                snapshot.clickFlat += Math.max(0.0D, value);
                break;
            case CLICK_DAMAGE_MULTIPLIER:
                snapshot.clickMultiplier *= Math.max(0.0D, value);
                break;
            case POISON_DAMAGE_FLAT:
                snapshot.poisonFlat += Math.max(0.0D, value);
                break;
            case POISON_DAMAGE_MULTIPLIER:
                snapshot.poisonMultiplier *= Math.max(0.0D, value);
                break;
            case CLICK_VULNERABILITY:
                snapshot.clickVulnerabilityPercent += value;
                break;
            case POISON_VULNERABILITY:
                snapshot.poisonVulnerabilityPercent += value;
                break;
            case ALL_DAMAGE_VULNERABILITY:
                snapshot.allDamageVulnerabilityPercent += value;
                break;
            case ALL_DAMAGE_MULTIPLIER:
                snapshot.allDamageMultiplier *= Math.max(0.0D, value);
                break;
            default:
                break;
        }
    }
}
