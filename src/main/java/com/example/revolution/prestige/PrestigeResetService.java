package com.example.revolution.prestige;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Centralized, runId-guarded reset transaction coordinator. */
public final class PrestigeResetService {
    public interface SaveMultiplierProvider {
        BigDecimal multiplier(UUID playerUuid, PrestigeBranch branch);
    }

    private final PrestigeRepository repository;
    private final SaveMultiplierProvider saveMultipliers;

    public PrestigeResetService(PrestigeRepository repository, PrestigeBranchRegistry registry, SaveMultiplierProvider saveMultipliers) {
        this.repository = repository;
        this.saveMultipliers = saveMultipliers;
    }

    public ResetPreview preview(UUID playerUuid, String expectedRunId) {
        PrestigeState state = repository.load(playerUuid);
        PrestigeState.RunState run = requireRun(state, expectedRunId);
        PrestigeState.BranchState branch = state.branch(run.activeBranch);
        BigDecimal multiplier = normalizedMultiplier(saveMultipliers.multiplier(playerUuid, run.activeBranch));
        BigDecimal candidate = run.branchProgress.multiply(multiplier);
        BigDecimal newBest = branch.bestSavedProgress.max(candidate);
        List<String> upgraded = upgradedRanks(run, branch);
        return new ResetPreview(run.runId, run.activeBranch, run.branchProgress, branch.bestSavedProgress,
                candidate, newBest, upgraded);
    }

    public ResetResult reset(UUID playerUuid, String expectedRunId) {
        PrestigeState current = repository.load(playerUuid);
        if (current.activeRun == null) {
            if (expectedRunId != null && expectedRunId.equals(current.lastResetRunId)) return ResetResult.DUPLICATE;
            return ResetResult.STALE;
        }
        if (expectedRunId == null || !expectedRunId.equals(current.activeRun.runId)) return ResetResult.STALE;

        PrestigeState next = current.copy();
        PrestigeState.RunState run = next.activeRun;
        PrestigeState.BranchState branch = next.branch(run.activeBranch);
        BigDecimal multiplier = normalizedMultiplier(saveMultipliers.multiplier(playerUuid, run.activeBranch));
        BigDecimal candidate = run.branchProgress.multiply(multiplier);
        branch.bestSavedProgress = branch.bestSavedProgress.max(candidate);
        for (String rankId : run.reachedRanks) {
            int previous = branch.rankMetaLevels.containsKey(rankId) ? branch.rankMetaLevels.get(rankId) : 0;
            branch.rankMetaLevels.put(rankId, Math.min(branch.rankMetaLevelCap, previous + 1));
        }
        next.lastResetRunId = run.runId;
        next.activeRun = null;

        PrestigeRepository.ResetCommitResult committed = repository.commitReset(playerUuid, expectedRunId, next);
        if (committed == PrestigeRepository.ResetCommitResult.APPLIED) return ResetResult.APPLIED;
        if (committed == PrestigeRepository.ResetCommitResult.DUPLICATE) return ResetResult.DUPLICATE;
        return ResetResult.STALE;
    }

    private PrestigeState.RunState requireRun(PrestigeState state, String expectedRunId) {
        if (state.activeRun == null) throw new IllegalStateException("No active Prestige run");
        if (expectedRunId != null && !expectedRunId.equals(state.activeRun.runId)) {
            throw new IllegalStateException("Stale Prestige runId");
        }
        return state.activeRun;
    }

    private List<String> upgradedRanks(PrestigeState.RunState run, PrestigeState.BranchState branch) {
        List<String> result = new ArrayList<>();
        for (String rankId : run.reachedRanks) {
            int previous = branch.rankMetaLevels.containsKey(rankId) ? branch.rankMetaLevels.get(rankId) : 0;
            if (previous < branch.rankMetaLevelCap) result.add(rankId + " " + previous + "->" + (previous + 1));
        }
        return Collections.unmodifiableList(result);
    }

    private BigDecimal normalizedMultiplier(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ONE : value;
    }

    public enum ResetResult { APPLIED, DUPLICATE, STALE }

    public static final class ResetPreview {
        public final String runId;
        public final PrestigeBranch branch;
        public final BigDecimal currentResult;
        public final BigDecimal previousBest;
        public final BigDecimal savedCandidate;
        public final BigDecimal newBest;
        public final List<String> ranksUpgraded;

        private ResetPreview(String runId, PrestigeBranch branch, BigDecimal currentResult, BigDecimal previousBest,
                             BigDecimal savedCandidate, BigDecimal newBest, List<String> ranksUpgraded) {
            this.runId = runId;
            this.branch = branch;
            this.currentResult = currentResult;
            this.previousBest = previousBest;
            this.savedCandidate = savedCandidate;
            this.newBest = newBest;
            this.ranksUpgraded = ranksUpgraded;
        }
    }
}
