package com.example.revolution.god;

import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.Preference;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic server-side Offering settlement and God automation.
 * One tick performs at most one Auto Worship spend per God, preventing reconnect/tick catch-up bursts.
 */
public final class GodWorshipAutomationService {
    public interface Policy extends GodWorshipService.Policy {
        boolean featureEnabled(FeatureGate gate);
        BigDecimal offeringGenerationPerSecond();
        long autoWorshipIntervalMillis();
        BigDecimal autoWorshipCooldownCost();
        long autoWorshipCooldownReductionMillis();
        BigDecimal autoWorshipChargeCost();
        long autoWorshipChargeGain();
        BigDecimal durationUpgradeCost(long currentLevel);
    }

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000L);
    private final GodWorshipRepository repository;
    private final Policy policy;
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;

    public GodWorshipAutomationService(GodWorshipRepository repository, Policy policy) {
        if (repository == null || policy == null) throw new IllegalArgumentException("God automation dependencies are required");
        this.repository = repository;
        this.policy = policy;
    }

    public void setGameplayEventSink(GameplayEventSink sink) {
        this.gameplayEvents = sink == null ? GameplayEventSink.NONE : sink;
    }

    public long processingIntervalMillis() { return policy.autoWorshipIntervalMillis(); }

    /** Used on login and regular server cadence. Settles active time before phase normalization. */
    public synchronized TickResult tick(UUID playerUuid, long nowMillis) {
        if (playerUuid == null) return TickResult.EMPTY;
        RunState run = repository.loadRunState(playerUuid);
        PersistentState persistent = repository.loadPersistentState(playerUuid);

        BigDecimal earned = settleOfferings(playerUuid, run, persistent, nowMillis);
        normalize(run, nowMillis);

        BigDecimal spent = BigDecimal.ZERO;
        int activations = 0;
        if (policy.featureEnabled(FeatureGate.GOD_AUTO_ACTIVATION)) {
            activations += activateReady(playerUuid, run, persistent, nowMillis);
        }
        for (GodId god : GodId.values()) {
            Preference preference = persistent.preferences.get(god);
            State state = run.gods.get(god);
            if (preference == null || state == null || !state.unlocked || !preference.autoWorshipEnabled) continue;
            BigDecimal cost = state.phase == Phase.COOLDOWN
                    ? policy.autoWorshipCooldownCost()
                    : state.phase == Phase.CHARGE ? policy.autoWorshipChargeCost() : BigDecimal.ZERO;
            if (cost.signum() <= 0 || persistent.offerings.compareTo(cost) < 0) continue;

            boolean changed = false;
            if (state.phase == Phase.COOLDOWN) {
                long reduced = GodWorshipRules.reduceCooldown(state, nowMillis, policy.autoWorshipCooldownReductionMillis());
                changed = reduced > 0L;
            } else if (state.phase == Phase.CHARGE) {
                long required = policy.chargeRequired(god);
                long before = state.chargeProgress;
                state.chargeProgress = GodWorshipRules.automatedChargeTarget(
                        before,
                        policy.autoWorshipChargeGain(),
                        required,
                        policy.chargePerClick(god),
                        preference.activationMode,
                        policy.featureEnabled(FeatureGate.GOD_AUTO_ACTIVATION));
                changed = state.chargeProgress != before;
            }
            if (!changed) continue;

            persistent.offerings = persistent.offerings.subtract(cost);
            spent = spent.add(cost);
            emit(playerUuid, GameplayEventType.GOD_AUTO_WORSHIP_SPEND, god, cost);
            emit(playerUuid, GameplayEventType.OFFERINGS_SPENT, god, cost);
        }

        if (policy.featureEnabled(FeatureGate.GOD_AUTO_ACTIVATION)) {
            activations += activateReady(playerUuid, run, persistent, nowMillis);
        }
        repository.saveRunState(playerUuid, run);
        repository.savePersistentState(playerUuid, persistent);
        return new TickResult(earned, spent, activations);
    }

    public synchronized boolean setAutoWorship(UUID playerUuid, GodId god, boolean enabled) {
        if (playerUuid == null || god == null) return false;
        PersistentState persistent = repository.loadPersistentState(playerUuid);
        persistent.preferences.get(god).autoWorshipEnabled = enabled;
        repository.savePersistentState(playerUuid, persistent);
        return true;
    }

    public synchronized boolean setActivationMode(UUID playerUuid, GodId god, ActivationMode mode, GodId linkedGod) {
        if (playerUuid == null || god == null || mode == null) return false;
        PersistentState persistent = repository.loadPersistentState(playerUuid);
        Preference preference = persistent.preferences.get(god);
        ActivationMode oldMode = preference.activationMode;
        GodId oldLinked = preference.linkedGodId;
        preference.activationMode = mode;
        preference.linkedGodId = mode == ActivationMode.LINKED_TO_GOD ? linkedGod : null;
        try {
            preference.validateFor(god);
        } catch (RuntimeException invalid) {
            preference.activationMode = oldMode;
            preference.linkedGodId = oldLinked;
            return false;
        }
        repository.savePersistentState(playerUuid, persistent);
        return true;
    }

    public synchronized boolean purchasePermanentDuration(UUID playerUuid) {
        if (playerUuid == null || !policy.featureEnabled(FeatureGate.GOD_PERMANENT_BUFF_DURATION)) return false;
        PersistentState persistent = repository.loadPersistentState(playerUuid);
        BigDecimal cost = policy.durationUpgradeCost(persistent.permanentDurationLevel);
        if (cost.signum() < 0 || persistent.offerings.compareTo(cost) < 0 || persistent.permanentDurationLevel == Long.MAX_VALUE)
            return false;
        persistent.offerings = persistent.offerings.subtract(cost);
        persistent.permanentDurationLevel++;
        repository.savePersistentState(playerUuid, persistent);
        emit(playerUuid, GameplayEventType.OFFERINGS_SPENT, null, cost);
        return true;
    }

    private BigDecimal settleOfferings(UUID playerUuid, RunState run, PersistentState persistent, long nowMillis) {
        BigDecimal rate = policy.offeringGenerationPerSecond();
        if (rate.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal earned = BigDecimal.ZERO;
        for (GodId god : GodId.values()) {
            State state = run.gods.get(god);
            if (state == null || state.phase != Phase.BUFF_ACTIVE || state.buffActiveUntilMillis <= 0L) continue;
            long duration = safeAdd(policy.buffDurationMillis(god), policy.permanentDurationMillis(persistent.permanentDurationLevel));
            long inferredStart = Math.max(0L, state.buffActiveUntilMillis - duration);
            long from = state.offeringSettledUntilMillis > 0L ? state.offeringSettledUntilMillis : inferredStart;
            long end = Math.min(nowMillis, state.buffActiveUntilMillis);
            if (end <= from) continue;
            long activeMillis = end - from;
            BigDecimal delta = rate.multiply(BigDecimal.valueOf(activeMillis)).divide(THOUSAND, 12, RoundingMode.DOWN);
            if (delta.signum() > 0) {
                earned = earned.add(delta);
                persistent.offerings = persistent.offerings.add(delta);
            }
            state.offeringSettledUntilMillis = end;
            if (delta.signum() > 0) emit(playerUuid, GameplayEventType.OFFERINGS_EARNED, god, delta);
        }
        return earned;
    }

    private void normalize(RunState run, long nowMillis) {
        for (GodId god : GodId.values()) {
            State state = run.gods.get(god);
            Phase before = state.phase;
            GodWorshipRules.normalize(state, nowMillis, policy.cooldownDurationMillis(god));
            if (before == Phase.BUFF_ACTIVE && state.phase != Phase.BUFF_ACTIVE) state.offeringSettledUntilMillis = 0L;
        }
    }

    private int activateReady(UUID playerUuid, RunState run, PersistentState persistent, long nowMillis) {
        int activations = 0;
        for (GodId god : GodId.values()) {
            Preference preference = persistent.preferences.get(god);
            if (preference != null && preference.activationMode == ActivationMode.AUTO_WHEN_READY) {
                activations += activateCascade(playerUuid, god, run, persistent, nowMillis,
                        EnumSet.noneOf(GodId.class), false);
            }
        }
        return activations;
    }

    private int activateCascade(UUID playerUuid, GodId god, RunState run, PersistentState persistent,
                                long nowMillis, Set<GodId> visited, boolean linkedTrigger) {
        if (god == null || visited.size() >= GodId.values().length || !visited.add(god)) return 0;
        State state = run.gods.get(god);
        Preference preference = persistent.preferences.get(god);
        if (state == null || preference == null || !policy.enabled(god)
                || !GodWorshipRules.readyToActivate(state, policy.chargeRequired(god))) return 0;

        if (!linkedTrigger && preference.activationMode != ActivationMode.AUTO_WHEN_READY) return 0;
        if (linkedTrigger && preference.activationMode != ActivationMode.LINKED_TO_GOD) return 0;

        GodWorshipRules.activate(state, nowMillis, policy.buffDurationMillis(god),
                policy.permanentDurationMillis(persistent.permanentDurationLevel));
        state.offeringSettledUntilMillis = nowMillis;
        emit(playerUuid, GameplayEventType.GOD_BUFF_ACTIVATE, god, BigDecimal.ONE);
        if (linkedTrigger) emit(playerUuid, GameplayEventType.GOD_LINKED_ACTIVATION, god, BigDecimal.ONE);
        int count = 1;

        for (GodId candidate : GodId.values()) {
            Preference candidatePreference = persistent.preferences.get(candidate);
            if (candidatePreference != null
                    && candidatePreference.activationMode == ActivationMode.LINKED_TO_GOD
                    && candidatePreference.linkedGodId == god) {
                count += activateCascade(playerUuid, candidate, run, persistent, nowMillis, visited, true);
            }
        }
        return count;
    }

    private void emit(UUID playerUuid, GameplayEventType type, GodId god, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        Map<String, String> metadata = new LinkedHashMap<>();
        if (god != null) metadata.put("godId", god.name());
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, type, amount,
                Collections.unmodifiableMap(metadata), "god_worship"));
    }

    private static long safeAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    public static final class TickResult {
        public static final TickResult EMPTY = new TickResult(BigDecimal.ZERO, BigDecimal.ZERO, 0);
        public final BigDecimal offeringsEarned;
        public final BigDecimal offeringsSpent;
        public final int activations;

        public TickResult(BigDecimal offeringsEarned, BigDecimal offeringsSpent, int activations) {
            this.offeringsEarned = offeringsEarned == null ? BigDecimal.ZERO : offeringsEarned;
            this.offeringsSpent = offeringsSpent == null ? BigDecimal.ZERO : offeringsSpent;
            this.activations = Math.max(0, activations);
        }
    }
}
