package com.example.revolution.rune.definition;

import com.example.revolution.rune.rules.MobRewardBonus;
import java.math.BigDecimal;

/** Test-only bridge for assertions that cross private feature boundaries. Never shipped in the mod JAR. */
public final class RuneTestSupport {
    private RuneTestSupport() {}

    public static void checkStatFormatting() {
        if (!RuneStatType.CLICK_DAMAGE_FLAT.format(40_015.5D).equals("+40.02K")) {
            throw new AssertionError("Rune stat display should use the shared large number formatter");
        }
        if (!RuneStatType.POISON_DAMAGE_MULTIPLIER.format(3.0D).equals("x3")) {
            throw new AssertionError("Small multiplier formatting must stay unchanged");
        }
    }

    public static BigDecimal rewardAmount(BigDecimal amount, double percent) {
        return MobRewardBonus.apply(amount, percent);
    }
}
