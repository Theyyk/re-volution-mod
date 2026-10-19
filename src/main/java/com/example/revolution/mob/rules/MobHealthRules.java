package com.example.revolution.mob.rules;

import com.example.revolution.number.LargeNumbers;
import java.math.BigDecimal;

public final class MobHealthRules {
    private MobHealthRules() {}
    private static final double STATUS_EFFECT_RADIUS_SQ = 4.0D;
    private static final BigDecimal VANILLA_MAX_HEALTH = new BigDecimal("1024");
    public static boolean isWithinStatusRadiusSq(double distanceSq) { return Double.isFinite(distanceSq) && distanceSq >= 0.0D && distanceSq <= STATUS_EFFECT_RADIUS_SQ; }

    public static double nativeHealthFor(BigDecimal configuredHp) { return positive(configuredHp).min(VANILLA_MAX_HEALTH).doubleValue(); }

    public static BigDecimal remainingHealthAfterDamage(BigDecimal currentHp, double damage) {
        if (currentHp == null || currentHp.signum() <= 0) return BigDecimal.ZERO;
        if (!Double.isFinite(damage) || damage <= 0.0D) return currentHp;
        BigDecimal remaining = currentHp.subtract(BigDecimal.valueOf(damage));
        return remaining.signum() <= 0 ? BigDecimal.ZERO : LargeNumbers.normalize(remaining);
    }

    public static BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0) return BigDecimal.ONE;
        return LargeNumbers.normalize(value);
    }
}
