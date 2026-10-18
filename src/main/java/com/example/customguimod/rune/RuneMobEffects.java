package com.example.customguimod.rune;
import java.math.BigDecimal;
import java.util.UUID;

/** Managed-mob boundary; mutable combat snapshots and reward stat types stay private to this package. */
public final class RuneMobEffects {
    private RuneMobEffects() {}

    /** Null retains the existing distinction between no active status and zero damage. */
    public static Double poisonDamage(UUID player, long worldTick) {
        RuneCombatMath.Snapshot snapshot = RuneCombatService.resolve(player, worldTick);
        return snapshot == null ? null : RuneCombatMath.poisonDamage(snapshot);
    }

    public static BigDecimal rewardAmount(UUID player, String resource, BigDecimal baseAmount) {
        double bonusPercent = RuneRewardBonusService.percentFor(player, resource);
        return MobRewardBonus.apply(baseAmount, bonusPercent);
    }
}
