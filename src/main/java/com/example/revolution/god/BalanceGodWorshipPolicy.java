package com.example.revolution.god;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.god.GodWorshipModel.GodId;

import java.math.BigDecimal;

/** Reads every gameplay balance number from the active Balance Core snapshot. */
public final class BalanceGodWorshipPolicy implements GodWorshipService.Policy, GodWorshipAutomationService.Policy {
    private final BalanceService balance;

    public BalanceGodWorshipPolicy(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    @Override public boolean enabled(GodId god) { return bool(key(god, "enabled")); }
    @Override public BigDecimal unlockCost(GodId god) { return decimal(key(god, "unlock_cost")); }
    @Override public BigDecimal upgradeCost(GodId god, long currentLevel) {
        return balance.evaluateActiveCurve("god." + god.name() + ".upgrade_cost", curveIndex(currentLevel));
    }
    @Override public long chargeRequired(GodId god) { return positiveLong(key(god, "charge_required")); }
    @Override public long chargePerClick(GodId god) { return positiveLong(key(god, "charge_per_click")); }
    @Override public long buffDurationMillis(GodId god) { return nonNegativeLong(key(god, "buff.duration_ms")); }
    @Override public long cooldownDurationMillis(GodId god) { return nonNegativeLong(key(god, "cooldown_ms")); }
    @Override public long cooldownReductionPerClickMillis(GodId god) { return positiveLong(key(god, "cooldown_reduction_per_click_ms")); }
    @Override public long permanentDurationMillis(long permanentDurationLevel) {
        if (permanentDurationLevel <= 0L) return 0L;
        long secondsPerLevel = nonNegativeLong("god.duration.seconds_per_level");
        if (secondsPerLevel != 0L && permanentDurationLevel > Long.MAX_VALUE / secondsPerLevel / 1000L)
            return Long.MAX_VALUE;
        return permanentDurationLevel * secondsPerLevel * 1000L;
    }

    @Override public boolean featureEnabled(GodWorshipModel.FeatureGate gate) {
        return bool("god.feature." + gate.name());
    }

    public BigDecimal buffAdditiveMultiplier(GodId god, long level) {
        if (level <= 0L) return BigDecimal.ZERO;
        return balance.evaluateActiveCurve("god." + god.name() + ".buff_multiplier", curveIndex(level - 1L));
    }

    @Override public BigDecimal offeringGenerationPerSecond() { return decimal("god.offering.generation_per_second"); }
    @Override public long autoWorshipIntervalMillis() { return positiveLong("god.auto_worship.processing_interval_ms"); }
    @Override public BigDecimal autoWorshipCooldownCost() { return decimal("god.auto_worship.cooldown.offering_cost"); }
    @Override public long autoWorshipCooldownReductionMillis() { return positiveLong("god.auto_worship.cooldown.reduction_ms"); }
    @Override public BigDecimal autoWorshipChargeCost() { return decimal("god.auto_worship.charge.offering_cost"); }
    @Override public long autoWorshipChargeGain() { return positiveLong("god.auto_worship.charge.gain"); }
    @Override public BigDecimal durationUpgradeCost(long currentLevel) {
        return balance.evaluateActiveCurve("god.duration.upgrade_cost", curveIndex(currentLevel));
    }

    private String key(GodId god, String suffix) { return "god." + god.name() + "." + suffix; }
    private boolean bool(String id) { return Boolean.parseBoolean(balance.getActiveValue(id)); }
    private BigDecimal decimal(String id) {
        BigDecimal value = new BigDecimal(balance.getActiveValue(id));
        if (value.signum() < 0) throw new IllegalStateException("negative God balance value: " + id);
        return value;
    }
    private long nonNegativeLong(String id) {
        long value = Long.parseLong(balance.getActiveValue(id));
        if (value < 0L) throw new IllegalStateException("negative God balance value: " + id);
        return value;
    }
    private long positiveLong(String id) {
        long value = nonNegativeLong(id);
        if (value == 0L) throw new IllegalStateException("God balance value must be positive: " + id);
        return value;
    }
    private static int curveIndex(long value) {
        if (value <= 0L) return 0;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
