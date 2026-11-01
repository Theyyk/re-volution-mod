package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Server-authoritative Mastery run engine. Legacy Prestige names remain for compatibility. */
public final class PrestigeService implements EffectResolver.Source {
    public interface AppliedResetParticipant { void afterAppliedReset(UUID playerUuid); }
    public interface AppliedMasteryResetObserver { void afterAppliedMasteryReset(UUID playerUuid, MasteryResetMode mode); }
    public interface FragmentRewardProvider { BigDecimal rewardPerLevel(PrestigeBranch branch); }
    public enum FragmentSource { MASTERY, PET }
    private static final AppliedResetParticipant NO_RESET_PARTICIPANT = playerUuid -> { };
    private static final AppliedMasteryResetObserver NO_MASTERY_RESET_OBSERVER = (playerUuid, mode) -> { };
    private static final FragmentRewardProvider DEFAULT_FRAGMENT_REWARDS = branch -> BigDecimal.ONE;
    private static final Predicate<UUID> OPEN_ACCESS = playerUuid -> true;
    private final PrestigeRepository repository;
    private final PrestigeBranchRegistry registry;
    private final PrestigeResetService resets;
    private final AppliedResetParticipant appliedResetParticipant;
    private final AppliedMasteryResetObserver appliedMasteryResetObserver;
    private FragmentRewardProvider fragmentRewards = DEFAULT_FRAGMENT_REWARDS;
    private GameplayEventSink gameplayEvents = GameplayEventSink.NONE;
    private Predicate<UUID> effectAccessGate = OPEN_ACCESS;

    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry) {
        this(repository, registry, (player, branch) -> registry.get(branch).defaultSaveMultiplier,
                NO_RESET_PARTICIPANT, MasteryTierPolicy.compatibility(), NO_MASTERY_RESET_OBSERVER);
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry, PrestigeResetService.SaveMultiplierProvider saveMultipliers) {
        this(repository, registry, saveMultipliers, NO_RESET_PARTICIPANT,
                MasteryTierPolicy.compatibility(), NO_MASTERY_RESET_OBSERVER);
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers,
                           AppliedResetParticipant appliedResetParticipant) {
        this(repository, registry, saveMultipliers, appliedResetParticipant,
                MasteryTierPolicy.compatibility(), NO_MASTERY_RESET_OBSERVER);
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers,
                           AppliedResetParticipant appliedResetParticipant,
                           MasteryTierPolicy tierPolicy) {
        this(repository, registry, saveMultipliers, appliedResetParticipant, tierPolicy, NO_MASTERY_RESET_OBSERVER);
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers,
                           AppliedResetParticipant appliedResetParticipant,
                           MasteryTierPolicy tierPolicy,
                           AppliedMasteryResetObserver appliedMasteryResetObserver) {
        this.repository = repository;
        this.registry = registry;
        this.resets = new PrestigeResetService(repository, registry, saveMultipliers, tierPolicy);
        this.appliedResetParticipant = appliedResetParticipant == null ? NO_RESET_PARTICIPANT : appliedResetParticipant;
        this.appliedMasteryResetObserver = appliedMasteryResetObserver == null
                ? NO_MASTERY_RESET_OBSERVER : appliedMasteryResetObserver;
    }

    public synchronized void setFragmentRewardProvider(FragmentRewardProvider provider) {
        fragmentRewards = provider == null ? DEFAULT_FRAGMENT_REWARDS : provider;
    }

    public synchronized void setGameplayEventSink(GameplayEventSink gameplayEvents) {
        this.gameplayEvents = gameplayEvents == null ? GameplayEventSink.NONE : gameplayEvents;
    }

    /** v2.12.0 access gate affects passive Mastery benefits only; persistent/factual state is retained. */
    public synchronized void setEffectAccessGate(Predicate<UUID> gate) {
        effectAccessGate = gate == null ? OPEN_ACCESS : gate;
    }

    public synchronized PrestigeState.RunState startRun(UUID playerUuid, PrestigeBranch branch) {
        PrestigeState state = repository.load(playerUuid);
        if (state.activeRun != null) throw new IllegalStateException("A Mastery run is already active; reset it before switching branch");
        registry.get(branch);
        PrestigeState.RunState run = new PrestigeState.RunState();
        run.runId = UUID.randomUUID().toString();
        run.activeBranch = branch;
        run.startedAt = System.currentTimeMillis();
        state.activeRun = run;
        repository.save(playerUuid, state);
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.MASTERY_BRANCH_SELECTED,
                BigDecimal.ONE, Collections.singletonMap("branch", branch.name()), "mastery_selection"));
        return run.copy();
    }

    public synchronized void recordProgress(UUID playerUuid, PrestigeBranch sourceBranch, BigDecimal factualAmount) {
        if (factualAmount == null || factualAmount.signum() <= 0) return;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || run.activeBranch != sourceBranch) return;
        PrestigeBranchRegistry.BranchDefinition definition = registry.get(sourceBranch);
        int oldLevel = run.branchLevel;
        run.branchProgress = run.branchProgress.add(factualAmount);
        run.branchLevel = definition.levelFor(run.branchProgress);
        if (run.branchLevel > oldLevel) {
            BigDecimal rewardPerLevel = fragmentRewards.rewardPerLevel(sourceBranch);
            if (rewardPerLevel == null || rewardPerLevel.signum() < 0) throw new IllegalStateException("Fragment reward must be non-negative for " + sourceBranch);
            run.masteryPendingFragments = run.masteryPendingFragments.add(rewardPerLevel.multiply(BigDecimal.valueOf(run.branchLevel - oldLevel)));
        }
        for (PrestigeBranchRegistry.RankDefinition rank : definition.ranks) {
            if (run.branchProgress.compareTo(rank.unlockProgress) >= 0 && !run.reachedRanks.contains(rank.rankId)) run.reachedRanks.add(rank.rankId);
        }
        repository.save(playerUuid, state);
        gameplayEvents.factualEvent(new GameplayEvent(
                playerUuid, GameplayEventType.MASTERY_PROGRESS, factualAmount,
                Collections.singletonMap("branch", sourceBranch.name()), "mastery_progress"));
    }

    public synchronized boolean spendRunBranchCurrency(UUID playerUuid, String expectedRunId, BigDecimal amount) {
        if (playerUuid == null || expectedRunId == null || amount == null || amount.signum() < 0) return false;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId) || run.runBranchCurrency.compareTo(amount) < 0) return false;
        run.runBranchCurrency = run.runBranchCurrency.subtract(amount);
        repository.save(playerUuid, state);
        return true;
    }

    public synchronized boolean recordRunExchangePurchase(UUID playerUuid, String expectedRunId, String offerId, int expectedPurchaseCount) {
        if (playerUuid == null || expectedRunId == null || offerId == null || offerId.trim().isEmpty() || expectedPurchaseCount < 0) return false;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId)) return false;
        int current = run.runExchangePurchaseCounts.containsKey(offerId) ? run.runExchangePurchaseCounts.get(offerId) : 0;
        if (current != expectedPurchaseCount || current == Integer.MAX_VALUE) return false;
        run.runExchangePurchaseCounts.put(offerId, current + 1);
        repository.save(playerUuid, state);
        return true;
    }

    public synchronized boolean addPetPendingFragments(UUID playerUuid, String expectedRunId, BigDecimal amount) {
        if (playerUuid == null || expectedRunId == null || amount == null || amount.signum() <= 0) return false;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId)) return false;
        run.petPendingFragments = run.petPendingFragments.add(amount);
        repository.save(playerUuid, state);
        return true;
    }

    public synchronized BigDecimal collectFragments(UUID playerUuid, String expectedRunId, FragmentSource source) {
        if (playerUuid == null || expectedRunId == null || source == null) return BigDecimal.ZERO;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId)) return BigDecimal.ZERO;
        BigDecimal pending = source == FragmentSource.MASTERY ? run.masteryPendingFragments : run.petPendingFragments;
        if (pending == null || pending.signum() <= 0) return BigDecimal.ZERO;
        run.runBranchCurrency = run.runBranchCurrency.add(pending);
        if (source == FragmentSource.MASTERY) run.masteryPendingFragments = BigDecimal.ZERO;
        else run.petPendingFragments = BigDecimal.ZERO;
        repository.save(playerUuid, state);
        gameplayEvents.factualEvent(new GameplayEvent(
                playerUuid, GameplayEventType.FRAGMENTS_COLLECTED, pending,
                Collections.singletonMap("source", source.name()), "fragment_collection"));
        if (source == FragmentSource.MASTERY) {
            gameplayEvents.factualEvent(new GameplayEvent(
                    playerUuid, GameplayEventType.FRAGMENTS_COLLECTED_MASTERY, pending,
                    Collections.singletonMap("source", source.name()), "mastery_fragment_collection"));
        }
        return pending;
    }

    public synchronized PrestigeState.RunState activeRun(UUID playerUuid) {
        PrestigeState state = repository.load(playerUuid);
        return state.activeRun == null ? null : state.activeRun.copy();
    }

    public synchronized PrestigeResetService.ResetPreview previewReset(UUID playerUuid, String expectedRunId) { return resets.preview(playerUuid, expectedRunId); }
    public synchronized PrestigeResetService.ResetResult reset(UUID playerUuid, String expectedRunId) {
        PrestigeState.RunState before = activeRun(playerUuid);
        PrestigeResetService.ResetResult result = resets.reset(playerUuid, expectedRunId);
        if (result == PrestigeResetService.ResetResult.APPLIED) {
            appliedResetParticipant.afterAppliedReset(playerUuid);
            emitMasteryReset(playerUuid, before);
        }
        return result;
    }

    public synchronized boolean masteryResetAvailable(UUID playerUuid) { return resets.masteryResetAvailable(playerUuid); }
    public synchronized PrestigeResetService.MasteryResetPreview previewMasteryReset(UUID playerUuid, String expectedRunId) {
        return resets.previewMasteryReset(playerUuid, expectedRunId);
    }
    public synchronized PrestigeResetService.MasteryResetResult masteryReset(UUID playerUuid, String expectedRunId) {
        return masteryReset(playerUuid, expectedRunId, MasteryResetMode.NORMAL);
    }
    public synchronized PrestigeResetService.MasteryResetResult masteryReset(UUID playerUuid, String expectedRunId, MasteryResetMode mode) {
        PrestigeState.RunState before = activeRun(playerUuid);
        boolean preserveRunes = false;
        if (mode == MasteryResetMode.PREMIUM_PRESERVE) {
            try {
                PrestigeResetService.MasteryResetPreview preview = resets.previewMasteryReset(playerUuid, expectedRunId);
                preserveRunes = preview.premiumPreserveEnabled && preview.premiumPreserveRunes;
            } catch (RuntimeException ignored) { preserveRunes = false; }
        }
        PrestigeResetService.MasteryResetResult result = resets.masteryReset(playerUuid, expectedRunId, mode);
        if (result == PrestigeResetService.MasteryResetResult.APPLIED) {
            if (!preserveRunes) appliedResetParticipant.afterAppliedReset(playerUuid);
            appliedMasteryResetObserver.afterAppliedMasteryReset(playerUuid, mode);
            emitMasteryReset(playerUuid, before);
        }
        return result;
    }

    private void emitMasteryReset(UUID playerUuid, PrestigeState.RunState before) {
        gameplayEvents.factualEvent(new GameplayEvent(playerUuid, GameplayEventType.MASTERY_RESET));
        if (before != null && before.activeBranch != null) {
            gameplayEvents.factualEvent(new GameplayEvent(
                    playerUuid, GameplayEventType.MASTERY_RESET_SPECIFIC_BRANCH, BigDecimal.ONE,
                    Collections.singletonMap("branch", before.activeBranch.name()), "mastery_reset"));
        }
    }

    public synchronized int metaLevelGainPerRun(UUID playerUuid, PrestigeBranch branch) {
        return resets.metaLevelGainPerRun(repository.load(playerUuid).branch(branch));
    }

    public synchronized PrestigeState snapshot(UUID playerUuid) { return repository.load(playerUuid).copy(); }
    public List<PrestigeBranch> branches() { return registry.branches(); }
    public String progressSource(PrestigeBranch branch) { return registry.get(branch).progressSource.toString(); }

    @Override public synchronized List<EffectResolver.Contribution> contributions(UUID playerUuid, EffectResolver.Stat stat) {
        if (!effectAccessGate.test(playerUuid)) return Collections.emptyList();
        PrestigeState state = repository.load(playerUuid);
        List<EffectResolver.Contribution> result = new ArrayList<>();
        for (PrestigeBranch branch : registry.branches()) {
            PrestigeState.BranchState permanent = state.branch(branch);
            PrestigeBranchRegistry.BranchDefinition definition = registry.get(branch);
            if (definition.permanentEffectStat != stat || definition.permanentBonusPerLevel.signum() <= 0) continue;
            int permanentLevels = definition.levelFor(permanent.bestSavedProgress);
            if (permanentLevels > 0) result.add(new EffectResolver.Contribution(stat, definition.permanentBonusPerLevel.multiply(BigDecimal.valueOf(permanentLevels)), "mastery:permanent:" + branch));
        }
        PrestigeState.RunState run = state.activeRun;
        if (run == null) return Collections.unmodifiableList(result);
        PrestigeState.BranchState permanent = state.branch(run.activeBranch);
        PrestigeBranchRegistry.BranchDefinition definition = registry.get(run.activeBranch);
        for (PrestigeBranchRegistry.RankDefinition rank : definition.ranks) {
            if (rank.effectStat != stat || !run.reachedRanks.contains(rank.rankId)) continue;
            int meta = permanent.rankMetaLevels.containsKey(rank.rankId) ? permanent.rankMetaLevels.get(rank.rankId) : 0;
            result.add(new EffectResolver.Contribution(stat, rank.effectAtMetaLevel(meta), "mastery:run:" + run.runId + ":branch:" + run.activeBranch + ":rank:" + rank.rankId));
        }
        return Collections.unmodifiableList(result);
    }
}
