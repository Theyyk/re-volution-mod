package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.State;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative mutation service. Client adapters may submit intent only. */
public final class GodWorshipService {
    public interface Policy {
        boolean enabled(GodId god);
        String unlockResource(GodId god);
        String upgradeResource(GodId god);
        EffectResolver.Stat primaryChannel(GodId god);
        EffectResolver.Stat secondaryChannel(GodId god);
        BigDecimal unlockCost(GodId god);
        BigDecimal upgradeCost(GodId god, long currentLevel);
        long chargeRequired(GodId god);
        long chargePerClick(GodId god);
        long buffDurationMillis(GodId god);
        long cooldownDurationMillis(GodId god);
        long cooldownReductionPerClickMillis(GodId god);
        long permanentDurationMillis(long permanentDurationLevel);
    }

    private final GodWorshipRepository repository;
    private final ResourceService resources;
    private final Policy policy;
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;

    public GodWorshipService(GodWorshipRepository repository, ResourceService resources, Policy policy) {
        if (repository == null || resources == null || policy == null)
            throw new IllegalArgumentException("God Worship dependencies are required");
        this.repository = repository; this.resources = resources; this.policy = policy;
    }

    public void setGameplayEventSink(GameplayEventSink sink) { gameplayEvents = sink == null ? GameplayEventSink.NONE : sink; }

    public synchronized MutationResult unlock(UUID playerUuid, GodId god) {
        if (!valid(playerUuid, god) || !policy.enabled(god)) return MutationResult.REJECTED;
        GodWorshipModel.RunState run = repository.loadRunState(playerUuid); State state = run.gods.get(god);
        if (state.unlocked) return MutationResult.DUPLICATE;
        BigDecimal cost = nonNegative(policy.unlockCost(god)); String resource = policy.unlockResource(god);
        if (!resources.spendPlayerResource(playerUuid, resource, cost)) return MutationResult.INSUFFICIENT_RESOURCE;
        try {
            state.unlocked = true; state.level = 1L; state.phase = Phase.CHARGE; state.chargeProgress = 0L;
            state.buffActiveUntilMillis = 0L; state.cooldownUntilMillis = 0L; state.offeringSettledUntilMillis = 0L;
            state.passiveChargeRemainder = BigDecimal.ZERO; repository.saveRunState(playerUuid, run);
        } catch (RuntimeException failure) { resources.grantPlayerResource(playerUuid, resource, cost); throw failure; }
        emit(playerUuid, GameplayEventType.GOD_UNLOCK, god, BigDecimal.ONE); return MutationResult.APPLIED;
    }

    public synchronized MutationResult upgrade(UUID playerUuid, GodId god) {
        if (!valid(playerUuid, god) || !policy.enabled(god)) return MutationResult.REJECTED;
        GodWorshipModel.RunState run = repository.loadRunState(playerUuid); State state = run.gods.get(god);
        if (!state.unlocked || state.level < 1L) return MutationResult.REJECTED;
        BigDecimal cost = nonNegative(policy.upgradeCost(god, state.level)); String resource = policy.upgradeResource(god);
        if (!resources.spendPlayerResource(playerUuid, resource, cost)) return MutationResult.INSUFFICIENT_RESOURCE;
        try {
            if (state.level == Long.MAX_VALUE) throw new IllegalStateException("God level overflow");
            state.level++; repository.saveRunState(playerUuid, run);
        } catch (RuntimeException failure) { resources.grantPlayerResource(playerUuid, resource, cost); throw failure; }
        emit(playerUuid, GameplayEventType.GOD_UPGRADE, god, BigDecimal.ONE); return MutationResult.APPLIED;
    }

    /** Backward-compatible call for existing tests and internal callers. */
    public synchronized MutationResult click(UUID playerUuid, GodId god, long nowMillis) {
        return click(playerUuid, god, nowMillis, true);
    }

    /** Manual activation with v2.12.0 child-gated permanent duration. */
    public synchronized MutationResult click(UUID playerUuid, GodId god, long nowMillis, boolean permanentDurationUnlocked) {
        if (!valid(playerUuid, god) || !policy.enabled(god)) return MutationResult.REJECTED;
        GodWorshipModel.RunState run = repository.loadRunState(playerUuid);
        GodWorshipModel.PersistentState persistent = repository.loadPersistentState(playerUuid);
        State state = run.gods.get(god); normalizeOne(state, god, nowMillis);
        if (!state.unlocked) return MutationResult.REJECTED;

        if (state.phase == Phase.CHARGE) {
            long required = policy.chargeRequired(god); long gain = Math.max(0L, policy.chargePerClick(god)); long before = state.chargeProgress;
            state.chargeProgress = Math.min(required, safeAdd(before, gain));
            if (state.chargeProgress >= required) {
                long permanent = permanentDurationUnlocked ? policy.permanentDurationMillis(persistent.permanentDurationLevel) : 0L;
                GodWorshipRules.activate(state, nowMillis, policy.buffDurationMillis(god), permanent);
                state.offeringSettledUntilMillis = nowMillis; state.passiveChargeRemainder = BigDecimal.ZERO;
                emit(playerUuid, GameplayEventType.GOD_BUFF_ACTIVATE, god, BigDecimal.ONE);
            } else if (state.chargeProgress != before) {
                emit(playerUuid, GameplayEventType.GOD_CHARGE_CLICK, god, BigDecimal.valueOf(state.chargeProgress - before));
            }
            repository.saveRunState(playerUuid, run); return MutationResult.APPLIED;
        }

        if (state.phase == Phase.COOLDOWN) {
            long reduced = GodWorshipRules.reduceCooldown(state, nowMillis, policy.cooldownReductionPerClickMillis(god));
            if (reduced <= 0L) return MutationResult.REJECTED;
            repository.saveRunState(playerUuid, run);
            emit(playerUuid, GameplayEventType.GOD_COOLDOWN_REDUCTION_CLICK, god, BigDecimal.valueOf(reduced));
            return MutationResult.APPLIED;
        }
        return MutationResult.REJECTED;
    }

    public synchronized GodWorshipModel.RunState normalize(UUID playerUuid, long nowMillis) {
        GodWorshipModel.RunState run = repository.loadRunState(playerUuid); boolean changed = false;
        for (GodId god : GodId.values()) {
            State state = run.gods.get(god); Phase before = state.phase; long beforeBuff = state.buffActiveUntilMillis;
            normalizeOne(state, god, nowMillis);
            if (before == Phase.BUFF_ACTIVE && state.phase != Phase.BUFF_ACTIVE && beforeBuff > 0L)
                emit(playerUuid, GameplayEventType.GOD_BUFF_EXPIRE, god, BigDecimal.ONE);
            changed |= before != state.phase || beforeBuff != state.buffActiveUntilMillis;
        }
        if (changed) repository.saveRunState(playerUuid, run); return run;
    }

    public synchronized GodWorshipModel.RunState runState(UUID playerUuid) { return repository.loadRunState(playerUuid); }
    public synchronized GodWorshipModel.PersistentState persistentState(UUID playerUuid) { return repository.loadPersistentState(playerUuid); }
    public Policy policy() { return policy; }
    public synchronized void onMasteryReset(UUID playerUuid, boolean preserveGodProgression) { if (playerUuid != null && !preserveGodProgression) repository.resetRunState(playerUuid); }
    private void normalizeOne(State state, GodId god, long nowMillis) { GodWorshipRules.normalize(state, nowMillis, policy.cooldownDurationMillis(god)); }
    private void emit(UUID playerUuid, GameplayEventType type, GodId god, BigDecimal amount) {
        Map<String, String> metadata = new LinkedHashMap<>(); metadata.put("godId", god.name());
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, type, amount, Collections.unmodifiableMap(metadata), "god_worship"));
    }
    private static BigDecimal nonNegative(BigDecimal value) { if (value == null || value.signum() < 0) throw new IllegalStateException("negative God balance value"); return value; }
    private static boolean valid(UUID playerUuid, GodId god) { return playerUuid != null && god != null; }
    private static long safeAdd(long a, long b) { return b > 0L && a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    public enum MutationResult { APPLIED, DUPLICATE, REJECTED, INSUFFICIENT_RESOURCE }
}
