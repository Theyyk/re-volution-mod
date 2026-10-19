package com.example.revolution.rune.definition;

/**
 * Data-driven rune stat definition plus a calculation breakdown prepared for
 * future detailed tooltips. v1.2.9 renders only finalValue.
 */
public final class RuneStat {
    public final RuneStatType type;
    public final double baseValue;
    public final boolean scalesWithAwakening;
    public final boolean scalesWithEnhancement;

    public RuneStat(RuneStatType type, double baseValue, boolean scalesWithAwakening, boolean scalesWithEnhancement) {
        if (type == null) throw new IllegalArgumentException("Rune stat type is required");
        if (!Double.isFinite(baseValue)) throw new IllegalArgumentException("Rune stat value must be finite");
        this.type = type;
        this.baseValue = baseValue;
        this.scalesWithAwakening = scalesWithAwakening;
        this.scalesWithEnhancement = scalesWithEnhancement;
    }

    public static RuneStat scaled(RuneStatType type, double baseValue) {
        return new RuneStat(type, baseValue, true, true);
    }

    public Breakdown resolve(int rank, double enhancementMultiplier) {
        return resolve(rank, enhancementMultiplier, 0.0D);
    }

    /**
     * Targeted power is added to the rune's starting value before rank and
     * enhancement scaling, matching the reference "+N to the power of <rune>" mechanic.
     */
    public Breakdown resolve(int rank, double enhancementMultiplier, double basePowerBonus) {
        double safeBasePowerBonus = Double.isFinite(basePowerBonus) ? basePowerBonus : 0.0D;
        double startingValue = baseValue + safeBasePowerBonus;

        double awakeningMultiplier = scalesWithAwakening ? rankMultiplier(rank) : 1.0D;
        double awakenedValue = startingValue * awakeningMultiplier;
        double awakeningBonus = awakenedValue - startingValue;

        double safeEnhancementMultiplier = Double.isFinite(enhancementMultiplier)
                ? Math.max(1.0D, enhancementMultiplier)
                : 1.0D;
        double enhancedValue = scalesWithEnhancement
                ? awakenedValue * safeEnhancementMultiplier
                : awakenedValue;
        double enhancementBonus = enhancedValue - awakenedValue;

        // Reserved for future sets and external post-scaling modifiers.
        double otherBonus = 0.0D;
        double finalValue = enhancedValue + otherBonus;
        return new Breakdown(startingValue, awakeningBonus, enhancementBonus, otherBonus, finalValue);
    }

    private static double rankMultiplier(int rank) {
        if (rank >= RuneLayoutDefinition.ADVANCED_RANK) return 9.0D;
        if (rank >= RuneLayoutDefinition.IMPROVED_RANK) return 3.0D;
        return 1.0D;
    }

    public static final class Breakdown {
        public final double baseValue;
        public final double awakeningBonus;
        public final double enhancementBonus;
        public final double otherBonus;
        public final double finalValue;

        Breakdown(double baseValue, double awakeningBonus, double enhancementBonus, double otherBonus, double finalValue) {
            this.baseValue = baseValue;
            this.awakeningBonus = awakeningBonus;
            this.enhancementBonus = enhancementBonus;
            this.otherBonus = otherBonus;
            this.finalValue = finalValue;
        }
    }
}
