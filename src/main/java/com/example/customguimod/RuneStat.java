package com.example.customguimod;

/**
 * Data-driven rune stat definition plus a calculation breakdown prepared for
 * future detailed tooltips. v1.2.9 renders only finalValue.
 */
final class RuneStat {
    final RuneStatType type;
    final double baseValue;
    final boolean scalesWithAwakening;
    final boolean scalesWithEnhancement;

    RuneStat(RuneStatType type, double baseValue, boolean scalesWithAwakening, boolean scalesWithEnhancement) {
        if (type == null) throw new IllegalArgumentException("Rune stat type is required");
        if (!Double.isFinite(baseValue)) throw new IllegalArgumentException("Rune stat value must be finite");
        this.type = type;
        this.baseValue = baseValue;
        this.scalesWithAwakening = scalesWithAwakening;
        this.scalesWithEnhancement = scalesWithEnhancement;
    }

    static RuneStat scaled(RuneStatType type, double baseValue) {
        return new RuneStat(type, baseValue, true, true);
    }

    Breakdown resolve(int rank, double enhancementMultiplier) {
        double awakeningMultiplier = scalesWithAwakening ? rankMultiplier(rank) : 1.0D;
        double awakenedValue = baseValue * awakeningMultiplier;
        double awakeningBonus = awakenedValue - baseValue;

        double safeEnhancementMultiplier = Double.isFinite(enhancementMultiplier)
                ? Math.max(1.0D, enhancementMultiplier)
                : 1.0D;
        double enhancedValue = scalesWithEnhancement
                ? awakenedValue * safeEnhancementMultiplier
                : awakenedValue;
        double enhancementBonus = enhancedValue - awakenedValue;

        // Reserved for future sets, temporary effects and external modifiers.
        double otherBonus = 0.0D;
        double finalValue = enhancedValue + otherBonus;
        return new Breakdown(baseValue, awakeningBonus, enhancementBonus, otherBonus, finalValue);
    }

    private static double rankMultiplier(int rank) {
        if (rank >= RuneInventory.ADVANCED_RANK) return 9.0D;
        if (rank >= RuneInventory.IMPROVED_RANK) return 3.0D;
        return 1.0D;
    }

    static final class Breakdown {
        final double baseValue;
        final double awakeningBonus;
        final double enhancementBonus;
        final double otherBonus;
        final double finalValue;

        Breakdown(double baseValue, double awakeningBonus, double enhancementBonus, double otherBonus, double finalValue) {
            this.baseValue = baseValue;
            this.awakeningBonus = awakeningBonus;
            this.enhancementBonus = enhancementBonus;
            this.otherBonus = otherBonus;
            this.finalValue = finalValue;
        }
    }
}
