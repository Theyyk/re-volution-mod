package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.State;

import java.math.BigDecimal;
import java.util.Collection;

/** Pure server-side rules used by network/service adapters and offline settlement. */
public final class GodWorshipRules {
    private GodWorshipRules() {}

    public static long manualChargeCap(long requiredCharge, long finalManualStep) {
        if (requiredCharge <= 0) throw new IllegalArgumentException("requiredCharge must be positive");
        if (finalManualStep <= 0 || finalManualStep > requiredCharge)
            throw new IllegalArgumentException("invalid finalManualStep");
        return requiredCharge - finalManualStep;
    }

    public static long automatedChargeTarget(long current, long requestedGain, long requiredCharge,
                                              long finalManualStep, ActivationMode mode, boolean autoActivationUnlocked) {
        long cap = requiredCharge;
        if (!autoActivationUnlocked || mode == ActivationMode.MANUAL)
            cap = manualChargeCap(requiredCharge, finalManualStep);
        return Math.max(0L, Math.min(cap, safeAdd(current, Math.max(0L, requestedGain))));
    }

    public static boolean readyToActivate(State state, long requiredCharge) {
        return state != null && state.unlocked && state.phase == Phase.CHARGE && state.chargeProgress >= requiredCharge;
    }

    public static void activate(State state, long nowMillis, long baseDurationMillis, long permanentBonusMillis) {
        if (state == null || state.phase != Phase.CHARGE) throw new IllegalStateException("God is not charging");
        long duration = safeAdd(Math.max(0L, baseDurationMillis), Math.max(0L, permanentBonusMillis));
        state.phase = Phase.BUFF_ACTIVE;
        state.chargeProgress = 0L;
        state.buffActiveUntilMillis = safeAdd(nowMillis, duration);
        state.cooldownUntilMillis = 0L;
    }

    /** Expired buffs enter cooldown at factual buff expiry, not at login/access time. */
    public static void normalize(State state, long nowMillis, long cooldownDurationMillis) {
        if (state == null || !state.unlocked) return;
        if (state.phase == Phase.BUFF_ACTIVE && nowMillis >= state.buffActiveUntilMillis) {
            long factualExpiry = state.buffActiveUntilMillis;
            state.buffActiveUntilMillis = 0L;
            state.cooldownUntilMillis = safeAdd(factualExpiry, Math.max(0L, cooldownDurationMillis));
            state.phase = nowMillis >= state.cooldownUntilMillis ? Phase.CHARGE : Phase.COOLDOWN;
            if (state.phase == Phase.CHARGE) state.cooldownUntilMillis = 0L;
        }
        if (state.phase == Phase.COOLDOWN && nowMillis >= state.cooldownUntilMillis) {
            state.phase = Phase.CHARGE;
            state.cooldownUntilMillis = 0L;
            state.chargeProgress = 0L;
        }
    }

    public static long reduceCooldown(State state, long nowMillis, long reductionMillis) {
        if (state == null || state.phase != Phase.COOLDOWN || reductionMillis <= 0L) return 0L;
        long before = Math.max(0L, state.cooldownUntilMillis - nowMillis);
        long reduced = Math.min(before, reductionMillis);
        state.cooldownUntilMillis -= reduced;
        if (state.cooldownUntilMillis <= nowMillis) {
            state.phase = Phase.CHARGE;
            state.cooldownUntilMillis = 0L;
            state.chargeProgress = 0L;
        }
        return reduced;
    }

    public static long factualActiveMillis(State state, long fromMillis, long toMillis) {
        if (state == null || state.phase != Phase.BUFF_ACTIVE || toMillis <= fromMillis) return 0L;
        long end = Math.min(toMillis, state.buffActiveUntilMillis);
        return Math.max(0L, end - fromMillis);
    }

    /** Combines God deltas inside the late God layer, then returns the layer multiplier. */
    public static BigDecimal godLayerMultiplier(Collection<BigDecimal> additiveGodDeltas) {
        BigDecimal result = BigDecimal.ONE;
        if (additiveGodDeltas != null) {
            for (BigDecimal delta : additiveGodDeltas) if (delta != null) result = result.add(delta);
        }
        return result.max(BigDecimal.ZERO);
    }

    /** Applies channel-specific God multiplier and ALL_DAMAGE exactly once to one factual damage instance. */
    public static BigDecimal applyGodLayer(BigDecimal preGodDamage, BigDecimal channelMultiplier,
                                           BigDecimal allDamageMultiplier) {
        if (preGodDamage == null) return BigDecimal.ZERO;
        BigDecimal channel = channelMultiplier == null ? BigDecimal.ONE : channelMultiplier;
        BigDecimal all = allDamageMultiplier == null ? BigDecimal.ONE : allDamageMultiplier;
        return preGodDamage.multiply(channel.max(BigDecimal.ZERO)).multiply(all.max(BigDecimal.ZERO));
    }

    public static boolean affects(GodId id, EffectResolver.Stat stat, boolean secondaryEnabled) {
        return GodWorshipModel.definition(id).channels(secondaryEnabled).contains(stat);
    }

    private static long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
