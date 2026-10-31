package com.example.revolution.prestige;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Centralized, runId-guarded reset transaction coordinator for ordinary runs and Mastery Reset. */
public final class PrestigeResetService {
    public interface SaveMultiplierProvider {
        BigDecimal multiplier(UUID playerUuid, PrestigeBranch branch);
    }

    private final PrestigeRepository repository;
    private final PrestigeBranchRegistry registry;
    private final SaveMultiplierProvider saveMultipliers;
    private final MasteryTierPolicy tierPolicy;

    public PrestigeResetService(PrestigeRepository repository, PrestigeBranchRegistry registry, SaveMultiplierProvider saveMultipliers) {
        this(repository, registry, saveMultipliers, MasteryTierPolicy.compatibility());
    }

    public PrestigeResetService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                                SaveMultiplierProvider saveMultipliers, MasteryTierPolicy tierPolicy) {
        if (repository == null) throw new IllegalArgumentException("prestige repository is required");
        if (registry == null) throw new IllegalArgumentException("prestige registry is required");
        if (saveMultipliers == null) throw new IllegalArgumentException("save multiplier provider is required");
        if (tierPolicy == null) throw new IllegalArgumentException("Mastery tier policy is required");
        this.repository = repository;
        this.registry = registry;
        this.saveMultipliers = saveMultipliers;
        this.tierPolicy = tierPolicy;
    }

    public ResetPreview preview(UUID playerUuid, String expectedRunId) {
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = requireRun(state, expectedRunId);
        PrestigeState.BranchState branch = state.branch(run.activeBranch);
        BigDecimal multiplier = normalizedMultiplier(saveMultipliers.multiplier(playerUuid, run.activeBranch));
        BigDecimal candidate = run.branchProgress.multiply(multiplier);
        BigDecimal newBest = branch.bestSavedProgress.max(candidate);
        int gain = currentTier(branch).metaLevelGainPerRun;
        List<String> upgraded = upgradedRanks(run, branch, gain);
        return new ResetPreview(run.runId, run.activeBranch, run.branchProgress, branch.bestSavedProgress,
                candidate, newBest, gain, upgraded);
    }

    public ResetResult reset(UUID playerUuid, String expectedRunId) {
        PrestigeState current = repository.load(playerUuid);
        ResetResult guard = ordinaryGuard(current, expectedRunId);
        if (guard != null) return guard;

        PrestigeState next = current.copy();
        PrestigeState.RunState run = next.activeRun;
        PrestigeState.BranchState branch = next.branch(run.activeBranch);
        applyRunResult(playerUuid, run, branch);
        next.lastResetRunId = run.runId;
        next.activeRun = null;

        PrestigeRepository.ResetCommitResult committed = repository.commitReset(playerUuid, expectedRunId, next);
        if (committed == PrestigeRepository.ResetCommitResult.APPLIED) return ResetResult.APPLIED;
        if (committed == PrestigeRepository.ResetCommitResult.DUPLICATE) return ResetResult.DUPLICATE;
        return ResetResult.STALE;
    }

    public boolean masteryResetAvailable(UUID playerUuid) {
        PrestigeState state = repository.load(playerUuid);
        if (state.activeRun == null || state.activeRun.activeBranch == null) return false;
        return masteryResetAvailable(state.activeRun.activeBranch, state.branch(state.activeRun.activeBranch));
    }

    public MasteryResetPreview previewMasteryReset(UUID playerUuid, String expectedRunId) {
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = requireRun(state, expectedRunId);
        PrestigeState.BranchState branch = state.branch(run.activeBranch);
        if (!masteryResetAvailable(run.activeBranch, branch)) throw new IllegalStateException("Mastery Reset is not available");
        MasteryTierPolicy.TierDefinition currentTier = currentTier(branch);
        MasteryTierPolicy.TierDefinition nextTier = nextTier(branch, currentTier);
        return new MasteryResetPreview(
                run.runId,
                run.activeBranch,
                branch.masteryTier,
                nextTier.tier,
                branch.rankMetaLevelCap,
                nextTier.rankMetaLevelCap,
                currentTier.metaLevelGainPerRun,
                nextTier.metaLevelGainPerRun,
                currentTier.premiumPreserveCrystalCost);
    }

    public MasteryResetResult masteryReset(UUID playerUuid, String expectedRunId) {
        PrestigeState current = repository.load(playerUuid);
        if (current.activeRun == null) {
            if (expectedRunId != null && expectedRunId.equals(current.lastResetRunId)) return MasteryResetResult.DUPLICATE;
            return MasteryResetResult.STALE;
        }
        if (expectedRunId == null || !expectedRunId.equals(current.activeRun.runId)) return MasteryResetResult.STALE;

        PrestigeState.BranchState currentBranch = current.branch(current.activeRun.activeBranch);
        if (!masteryResetAvailable(current.activeRun.activeBranch, currentBranch)) return MasteryResetResult.NOT_ELIGIBLE;

        MasteryTierPolicy.TierDefinition currentTier;
        MasteryTierPolicy.TierDefinition nextTier;
        try {
            currentTier = currentTier(currentBranch);
            nextTier = nextTier(currentBranch, currentTier);
        } catch (RuntimeException invalidPolicy) {
            return MasteryResetResult.POLICY_UNAVAILABLE;
        }

        PrestigeState next = current.copy();
        PrestigeState.RunState run = next.activeRun;
        PrestigeState.BranchState branch = next.branch(run.activeBranch);
        applyRunResult(playerUuid, run, branch);
        branch.masteryTier = nextTier.tier;
        branch.rankMetaLevelCap = nextTier.rankMetaLevelCap;
        next.lastResetRunId = run.runId;
        next.activeRun = null;

        PrestigeRepository.ResetCommitResult committed = repository.commitReset(playerUuid, expectedRunId, next);
        if (committed == PrestigeRepository.ResetCommitResult.APPLIED) return MasteryResetResult.APPLIED;
        if (committed == PrestigeRepository.ResetCommitResult.DUPLICATE) return MasteryResetResult.DUPLICATE;
        return MasteryResetResult.STALE;
    }

    public int metaLevelGainPerRun(PrestigeState.BranchState branch) {
        return currentTier(branch).metaLevelGainPerRun;
    }

    private ResetResult ordinaryGuard(PrestigeState current, String expectedRunId) {
        if (current.activeRun == null) {
            if (expectedRunId != null && expectedRunId.equals(current.lastResetRunId)) return ResetResult.DUPLICATE;
            return ResetResult.STALE;
        }
        if (expectedRunId == null || !expectedRunId.equals(current.activeRun.runId)) return ResetResult.STALE;
        return null;
    }

    private void applyRunResult(UUID playerUuid, PrestigeState.RunState run, PrestigeState.BranchState branch) {
        BigDecimal multiplier = normalizedMultiplier(saveMultipliers.multiplier(playerUuid, run.activeBranch));
        BigDecimal candidate = run.branchProgress.multiply(multiplier);
        branch.bestSavedProgress = branch.bestSavedProgress.max(candidate);
        int gain = currentTier(branch).metaLevelGainPerRun;
        for (String rankId : run.reachedRanks) {
            int previous = branch.rankMetaLevels.containsKey(rankId) ? branch.rankMetaLevels.get(rankId) : 0;
            branch.rankMetaLevels.put(rankId, Math.min(branch.rankMetaLevelCap, previous + gain));
        }
    }

    private boolean masteryResetAvailable(PrestigeBranch branchId, PrestigeState.BranchState branch) {
        PrestigeBranchRegistry.BranchDefinition definition = registry.get(branchId);
        if (definition.ranks.isEmpty()) return false;
        for (PrestigeBranchRegistry.RankDefinition rank : definition.ranks) {
            int meta = branch.rankMetaLevels.containsKey(rank.rankId) ? branch.rankMetaLevels.get(rank.rankId) : 0;
            if (meta < branch.rankMetaLevelCap) return false;
        }
        return true;
    }

    private MasteryTierPolicy.TierDefinition currentTier(PrestigeState.BranchState branch) {
        MasteryTierPolicy.TierDefinition definition = tierPolicy.tier(branch.masteryTier);
        if (definition.tier != branch.masteryTier) throw new IllegalStateException("Mastery tier policy returned a mismatched tier");
        return definition;
    }

    private MasteryTierPolicy.TierDefinition nextTier(PrestigeState.BranchState branch, MasteryTierPolicy.TierDefinition currentTier) {
        MasteryTierPolicy.TierDefinition next = tierPolicy.tier(branch.masteryTier + 1);
        if (next.tier != branch.masteryTier + 1) throw new IllegalStateException("Mastery tier policy returned a mismatched next tier");
        if (next.rankMetaLevelCap <= branch.rankMetaLevelCap) throw new IllegalStateException("next Mastery tier must increase rank meta-level cap");
        if (next.metaLevelGainPerRun <= currentTier.metaLevelGainPerRun) throw new IllegalStateException("next Mastery tier must increase meta-level gain per run");
        return next;
    }

    private PrestigeState.RunState requireRun(PrestigeState state, String expectedRunId) {
        if (state.activeRun == null) throw new IllegalStateException("No active Mastery run");
        if (expectedRunId != null && !expectedRunId.equals(state.activeRun.runId)) {
            throw new IllegalStateException("Stale Mastery runId");
        }
        return state.activeRun;
    }

    private List<String> upgradedRanks(PrestigeState.RunState run, PrestigeState.BranchState branch, int gain) {
        List<String> result = new ArrayList<>();
        for (String rankId : run.reachedRanks) {
            int previous = branch.rankMetaLevels.containsKey(rankId) ? branch.rankMetaLevels.get(rankId) : 0;
            if (previous < branch.rankMetaLevelCap) {
                int upgraded = Math.min(branch.rankMetaLevelCap, previous + gain);
                result.add(rankId + " " + previous + "->" + upgraded);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private BigDecimal normalizedMultiplier(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ONE : value;
    }

    public enum ResetResult { APPLIED, DUPLICATE, STALE }
    public enum MasteryResetResult { APPLIED, DUPLICATE, STALE, NOT_ELIGIBLE, POLICY_UNAVAILABLE }

    public static final class ResetPreview {
        public final String runId;
        public final PrestigeBranch branch;
        public final BigDecimal currentResult;
        public final BigDecimal previousBest;
        public final BigDecimal savedCandidate;
        public final BigDecimal newBest;
        public final int metaLevelGainPerRun;
        public final List<String> ranksUpgraded;

        private ResetPreview(String runId, PrestigeBranch branch, BigDecimal currentResult, BigDecimal previousBest,
                             BigDecimal savedCandidate, BigDecimal newBest, int metaLevelGainPerRun, List<String> ranksUpgraded) {
            this.runId = runId;
            this.branch = branch;
            this.currentResult = currentResult;
            this.previousBest = previousBest;
            this.savedCandidate = savedCandidate;
            this.newBest = newBest;
            this.metaLevelGainPerRun = metaLevelGainPerRun;
            this.ranksUpgraded = ranksUpgraded;
        }
    }

    public static final class MasteryResetPreview {
        public final String runId;
        public final PrestigeBranch branch;
        public final int currentMasteryTier;
        public final int nextMasteryTier;
        public final int currentRankMetaLevelCap;
        public final int nextRankMetaLevelCap;
        public final int currentMetaLevelGainPerRun;
        public final int nextMetaLevelGainPerRun;
        public final int premiumPreserveCrystalCost;

        private MasteryResetPreview(String runId, PrestigeBranch branch,
                                    int currentMasteryTier, int nextMasteryTier,
                                    int currentRankMetaLevelCap, int nextRankMetaLevelCap,
                                    int currentMetaLevelGainPerRun, int nextMetaLevelGainPerRun,
                                    int premiumPreserveCrystalCost) {
            this.runId = runId;
            this.branch = branch;
            this.currentMasteryTier = currentMasteryTier;
            this.nextMasteryTier = nextMasteryTier;
            this.currentRankMetaLevelCap = currentRankMetaLevelCap;
            this.nextRankMetaLevelCap = nextRankMetaLevelCap;
            this.currentMetaLevelGainPerRun = currentMetaLevelGainPerRun;
            this.nextMetaLevelGainPerRun = nextMetaLevelGainPerRun;
            this.premiumPreserveCrystalCost = premiumPreserveCrystalCost;
        }
    }
}
