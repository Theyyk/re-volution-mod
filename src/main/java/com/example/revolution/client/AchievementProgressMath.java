package com.example.revolution.client;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure achievement progress geometry; network values may use scientific notation. */
final class AchievementProgressMath {
    private AchievementProgressMath() {}

    static double fraction(String progress, String target, boolean completed) {
        try {
            BigDecimal goal = new BigDecimal(target);
            if (goal.signum() <= 0) return completed ? 1D : 0D;
            BigDecimal current = new BigDecimal(progress);
            double ratio = current.divide(goal, 6, RoundingMode.HALF_UP).doubleValue();
            return Math.max(0D, Math.min(1D, ratio));
        } catch (NumberFormatException | ArithmeticException invalid) {
            return completed ? 1D : 0D;
        }
    }
}
