package com.example.revolution.rune.service;

import com.example.revolution.rune.rules.MobRewardBonus;
import com.example.revolution.rune.rules.RuneCombatMath;
import java.math.BigDecimal;
import java.util.UUID;

/** Managed-mob boundary; mutable combat snapshots and reward stat types stay private to this package. */
public final class RuneMobEffectService {
    private final RuneCombatService combat;
    private final RuneRewardBonusService rewards;

    public RuneMobEffectService(RuneCombatService combat, RuneRewardBonusService rewards) {
        this.combat = combat;
        this.rewards = rewards;
    }

    /** Null retains the existing distinction between no active status and zero damage. */
    public Double poisonDamage(UUID player, long worldTick) {
        RuneCombatMath.Snapshot snapshot = combat.resolve(player, worldTick);
        return snapshot == null ? null : RuneCombatMath.poisonDamage(snapshot);
    }

    public BigDecimal rewardAmount(UUID player, String resource, BigDecimal baseAmount) {
        double bonusPercent = rewards.percentFor(player, resource);
        return MobRewardBonus.apply(baseAmount, bonusPercent);
    }
}
