package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Server-authoritative Mastery run engine. Legacy Prestige names remain for compatibility. */
public final class PrestigeService implements EffectResolver.Source {
    public interface AppliedResetParticipant { void afterAppliedReset(UUID playerUuid); }
    public interface FragmentRewardProvider { BigDecimal rewardPerLevel(PrestigeBranch branch); }
    public enum FragmentSource { MASTERY, PET }
    private static final AppliedResetParticipant NO_RESET_PARTICIPANT = playerUuid -> { };
    private static final FragmentRewardProvider DEFAULT_FRAGMENT_REWARDS = branch -> BigDecimal.ONE;
    private final PrestigeRepository repository;
    private final PrestigeBranchRegistry registry;
    private final PrestigeResetService resets;
    private final AppliedResetParticipant appliedResetParticipant;
    private FragmentRewardProvider fragmentRewards = DEFAULT_FRAGMENT_REWARDS;

    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry) {
        this(repository, registry, (player, branch) -> registry.get(branch).defaultSaveMultiplier,
                NO_RESET_PARTICIPANT, MasteryTierPolicy.compatibility());
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry, PrestigeResetService.SaveMultiplierProvider saveMultipliers) {
        this(repository, registry, saveMultipliers, NO_RESET_PARTICIPANT, MasteryTierPolicy.compatibility());
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers,
                           AppliedResetParticipant appliedResetParticipant) {
        this(repository, registry, saveMultipliers, appliedResetParticipant, MasteryTierPolicy.compatibility());
    }
    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers,
                           AppliedResetParticipant appliedResetParticipant,
                           MasteryTierPolicy tierPolicy) {
        this.repository = repository;
        this.registry = registry;
        this.resets = new PrestigeResetService(repository, registry, saveMultipliers, tierPolicy);
        this.appliedResetParticipant = appliedResetParticipant == null ? NO_RESET_PARTICIPANT : appliedResetParticipant;
    }

    public synchronized void setFragmentRewardProvider(FragmentRewardProvider provider) {
        fragmentRewards = provider == null ? DEFAULT_FRAGMENT_REWARDS : provider;
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
    }

    /** Compatibility name: spends the single run-scoped spendable Fragments balance. */
    public synchronized boolean spendRunBranchCurrency(UUID playerUuid, String expectedRunId, BigDecimal amount) {
        if (playerUuid == null || expectedRunId == null || amount == null || amount.signum() < 0) return false;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId) || run.runBranchCurrency.compareTo(amount) < 0) return false;
        run.runBranchCurrency = run.runBranchCurrency.subtract(amount);
        repository.save(playerUuid, state);
        return true;
    }

    /** Authoritative Pet reward path. Threshold reach alone never calls this method. */
    public synchronized boolean addPetPendingFragments(UUID playerUuid, String expectedRunId, BigDecimal amount) {
        if (playerUuid == null || expectedRunId == null || amount == null || amount.signum() <= 0) return false;
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = state.activeRun;
        if (run == null || !expectedRunId.equals(run.runId)) return false;
        run.petPendingFragments = run.petPendingFragments.add(amount);
        repository.save(playerUuid, state);
        return true;
    }

    /** Moves factual pending from exactly one source into the shared Fragments balance. */
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
        return pending;
    }

    public synchronized PrestigeState.RunState activeRun(UUID playerUuid) {
        PrestigeState state = repository.load(playerUuid);
        return state.activeRun == null ? null : state.activeRun.copy();
    }

    public synchronized PrestigeResetService.ResetPreview previewReset(UUID playerUuid, String expectedRunId) { return resets.preview(playerUuid, expectedRunId); }
    public synchronized PrestigeResetService.ResetResult reset(UUID playerUuid, String expectedRunId) {
        PrestigeResetService.ResetResult result = resets.reset(playerUuid, expectedRunId);
        if (result == PrestigeResetService.ResetResult.APPLIED) appliedResetParticipant.afterAppliedReset(playerUuid);
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
        boolean preserveRunes = false;
        if (mode == MasteryResetMode.PREMIUM_PRESERVE) {
            try {
                PrestigeResetService.MasteryResetPreview preview = resets.previewMasteryReset(playerUuid, expectedRunId);
                preserveRunes = preview.premiumPreserveEnabled && preview.premiumPreserveRunes;
            } catch (RuntimeException ignored) { preserveRunes = false; }
        }
        PrestigeResetService.MasteryResetResult result = resets.masteryReset(playerUuid, expectedRunId, mode);
        if (result == PrestigeResetService.MasteryResetResult.APPLIED && !preserveRunes) appliedResetParticipant.afterAppliedReset(playerUuid);
        return result;
    }
    public synchronized int metaLevelGainPerRun(UUID playerUuid, PrestigeBranch branch) {
        return resets.metaLevelGainPerRun(repository.load(playerUuid).branch(branch));
    }

    public synchronized PrestigeState snapshot(UUID playerUuid) { return repository.load(playerUuid).copy(); }
    public List<PrestigeBranch> branches() { return registry.branches(); }
    public String progressSource(PrestigeBranch branch) { return registry.get(branch).progressSource.toString(); }

    @Override public synchronized List<EffectResolver.Contribution> contributions(UUID playerUuid, EffectResolver.Stat stat) {
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
