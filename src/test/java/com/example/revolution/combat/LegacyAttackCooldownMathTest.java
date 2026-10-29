package com.example.revolution.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class LegacyAttackCooldownMathTest {
    @Test
    void fullyChargedHitIsUnchanged() {
        assertEquals(7.0D,
                LegacyAttackCooldownMath.normalizeToFullStrength(7.0D, 5.0D, 2.0D, 1.0D),
                1.0E-9D);
    }

    @Test
    void halfChargedBaseDamageReturnsToFullStrength() {
        double cooled = 0.5D;
        double scaled = 1.0D * LegacyAttackCooldownMath.vanillaBaseScale(cooled);
        assertEquals(1.0D,
                LegacyAttackCooldownMath.normalizeToFullStrength(scaled, 1.0D, 0.0D, cooled),
                1.0E-9D);
    }

    @Test
    void immediateRepeatedClickReturnsToFullStrength() {
        double scaled = 1.0D * LegacyAttackCooldownMath.vanillaBaseScale(0.0D);
        assertEquals(1.0D,
                LegacyAttackCooldownMath.normalizeToFullStrength(scaled, 1.0D, 0.0D, 0.0D),
                1.0E-9D);
    }

    @Test
    void enchantmentComponentUsesVanillaLinearCooldownThenNormalizes() {
        double cooled = 0.5D;
        double attack = 5.0D;
        double enchantment = 2.0D;
        double scaled = attack * LegacyAttackCooldownMath.vanillaBaseScale(cooled) + enchantment * cooled;
        assertEquals(7.0D,
                LegacyAttackCooldownMath.normalizeToFullStrength(scaled, attack, enchantment, cooled),
                1.0E-9D);
    }

    @Test
    void preservesOtherMultiplicativeEffectsProportionally() {
        double cooled = 0.5D;
        double attack = 4.0D;
        double scaled = attack * LegacyAttackCooldownMath.vanillaBaseScale(cooled);
        double externallyMultiplied = scaled * 1.5D;
        assertEquals(attack * 1.5D,
                LegacyAttackCooldownMath.normalizeToFullStrength(externallyMultiplied, attack, 0.0D, cooled),
                1.0E-9D);
    }
}
