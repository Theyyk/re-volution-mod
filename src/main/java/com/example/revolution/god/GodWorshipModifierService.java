package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Resolves the isolated late God layer without registering it as a normal EffectResolver source. */
public final class GodWorshipModifierService {
    public interface Policy {
        boolean enabled(GodId god);
        boolean featureEnabled(FeatureGate gate);
        EffectResolver.Stat primaryChannel(GodId god);
        EffectResolver.Stat secondaryChannel(GodId god);
        BigDecimal buffAdditiveMultiplier(GodId god, long level);
    }
    public interface EffectivenessProvider { BigDecimal multiplier(UUID playerUuid, GodId god, EffectResolver.Stat channel); }
    public static final EffectivenessProvider IDENTITY_EFFECTIVENESS = (playerUuid, god, channel) -> BigDecimal.ONE;
    private final GodWorshipRepository repository;
    private final Policy policy;
    private final EffectivenessProvider effectiveness;

    public GodWorshipModifierService(GodWorshipRepository repository, Policy policy) { this(repository, policy, IDENTITY_EFFECTIVENESS); }
    public GodWorshipModifierService(GodWorshipRepository repository, Policy policy, EffectivenessProvider effectiveness) {
        if (repository == null || policy == null || effectiveness == null) throw new IllegalArgumentException("God modifier dependencies are required");
        this.repository = repository; this.policy = policy; this.effectiveness = effectiveness;
    }

    /** Backward-compatible unrestricted child-feature behavior for existing tests. */
    public BigDecimal apply(UUID playerUuid, GameplayDamageSource source, BigDecimal preGodDamage, long nowMillis) {
        return apply(playerUuid, source, preGodDamage, nowMillis, true);
    }

    /** v2.12.0 runtime path: secondary channels require the dedicated child unlock. */
    public BigDecimal apply(UUID playerUuid, GameplayDamageSource source, BigDecimal preGodDamage, long nowMillis,
                            boolean secondaryBuffsUnlocked) {
        if (playerUuid == null || preGodDamage == null || preGodDamage.signum() <= 0)
            return preGodDamage == null ? BigDecimal.ZERO : preGodDamage;
        EffectResolver.Stat channel = statFor(source);
        RunState run = repository.loadRunState(playerUuid);
        boolean secondaryEnabled = secondaryBuffsUnlocked && policy.featureEnabled(FeatureGate.GOD_SECONDARY_BUFFS);
        List<BigDecimal> channelDeltas = new ArrayList<>();
        List<BigDecimal> allDeltas = new ArrayList<>();
        for (GodId god : GodId.values()) {
            State state = run.gods.get(god);
            if (state == null || !state.unlocked || state.level <= 0L || state.phase != Phase.BUFF_ACTIVE
                    || nowMillis >= state.buffActiveUntilMillis || !policy.enabled(god)) continue;
            BigDecimal baseDelta = policy.buffAdditiveMultiplier(god, state.level);
            if (baseDelta == null || baseDelta.signum() <= 0) continue;
            EffectResolver.Stat primary = policy.primaryChannel(god);
            EffectResolver.Stat secondary = secondaryEnabled ? policy.secondaryChannel(god) : null;
            if (primary == EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER) allDeltas.add(effectiveDelta(playerUuid, god, primary, baseDelta));
            if (secondary == EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER) allDeltas.add(effectiveDelta(playerUuid, god, secondary, baseDelta));
            if (channel != null && primary == channel) channelDeltas.add(effectiveDelta(playerUuid, god, primary, baseDelta));
            if (channel != null && secondary == channel) channelDeltas.add(effectiveDelta(playerUuid, god, secondary, baseDelta));
        }
        return GodWorshipRules.applyGodLayer(preGodDamage, GodWorshipRules.godLayerMultiplier(channelDeltas), GodWorshipRules.godLayerMultiplier(allDeltas));
    }

    private BigDecimal effectiveDelta(UUID playerUuid, GodId god, EffectResolver.Stat channel, BigDecimal baseDelta) {
        BigDecimal multiplier = effectiveness.multiplier(playerUuid, god, channel);
        if (multiplier == null || multiplier.signum() < 0) multiplier = BigDecimal.ONE;
        return baseDelta.multiply(multiplier);
    }

    public static EffectResolver.Stat statFor(GameplayDamageSource source) {
        if (source == null) return null;
        switch (source) {
            case CLICK: return EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER;
            case POISON: return EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER;
            case BURNING: return EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER;
            case BLEEDING: return EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER;
            case CURSE: return EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER;
            case WITHER: return EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER;
            case DECAY: return EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER;
            default: return null;
        }
    }
}
