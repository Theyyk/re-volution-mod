package com.example.revolution.combat;

/** Pure 1.9+ melee cooldown normalization used before RE:volution click-damage modifiers. */
public final class LegacyAttackCooldownMath {
    private LegacyAttackCooldownMath() {}

    public static double vanillaBaseScale(double cooledAttackStrength) {
        double cooled = clamp01(cooledAttackStrength);
        return 0.2D + cooled * cooled * 0.8D;
    }

    /**
     * Restores only the vanilla cooldown-reduced melee component to full strength.
     * The caller supplies the already-produced LivingHurt amount so critical/other multiplicative
     * effects remain proportional instead of being reimplemented here.
     */
    public static double normalizeToFullStrength(
            double actualDamage,
            double attackDamage,
            double enchantmentDamage,
            double cooledAttackStrength) {
        if (!Double.isFinite(actualDamage) || actualDamage <= 0.0D) return actualDamage;

        double attack = Math.max(0.0D, finiteOrZero(attackDamage));
        double enchantment = Math.max(0.0D, finiteOrZero(enchantmentDamage));
        double cooled = clamp01(cooledAttackStrength);
        double scaledVanilla = attack * vanillaBaseScale(cooled) + enchantment * cooled;
        double fullVanilla = attack + enchantment;

        if (scaledVanilla <= 0.0D || fullVanilla <= 0.0D) return actualDamage;
        double normalized = actualDamage * (fullVanilla / scaledVanilla);
        return Double.isFinite(normalized) && normalized >= 0.0D ? normalized : actualDamage;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        if (value <= 0.0D) return 0.0D;
        if (value >= 1.0D) return 1.0D;
        return value;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
