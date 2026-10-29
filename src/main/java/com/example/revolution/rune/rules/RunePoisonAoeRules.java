package com.example.revolution.rune.rules;

/** Pure v1.5.0 poison DPS budget distribution across simultaneous managed-mob targets. */
public final class RunePoisonAoeRules {
    public static final int TICKS_PER_SECOND = 5;
    public static final int MC_TICK_INTERVAL = 4;
    private static final double[] TOTAL_MULTIPLIER = {
            0.0D,
            1.00D,
            1.50D,
            1.80D,
            2.00D,
            2.20D,
            2.30D,
            2.40D,
            2.50D,
            2.60D,
            2.70D
    };

    private RunePoisonAoeRules() {}

    public static double totalMultiplier(int targetCount) {
        if (targetCount <= 0) return 0.0D;
        int capped = Math.min(targetCount, TOTAL_MULTIPLIER.length - 1);
        return TOTAL_MULTIPLIER[capped];
    }

    /** poisonDps is the total single-target DPS budget before AoE distribution. */
    public static double perTargetTick(double poisonDps, int targetCount) {
        if (!Double.isFinite(poisonDps) || poisonDps <= 0.0D || targetCount <= 0) return 0.0D;
        return poisonDps * totalMultiplier(targetCount) / targetCount / TICKS_PER_SECOND;
    }
}
