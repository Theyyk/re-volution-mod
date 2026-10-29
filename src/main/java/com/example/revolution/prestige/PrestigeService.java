package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Server-authoritative Prestige run engine. */
public final class PrestigeService implements EffectResolver.Source {
    private final PrestigeRepository repository;
    private final PrestigeBranchRegistry registry;
    private final PrestigeResetService resets;

    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry) {
        this(repository, registry, (player, branch) -> registry.get(branch).defaultSaveMultiplier);
    }

    public PrestigeService(PrestigeRepository repository, PrestigeBranchRegistry registry,
                           PrestigeResetService.SaveMultiplierProvider saveMultipliers) {
        this.repository = repository;
        this.registry = registry;
        this.resets = new PrestigeResetService(repository, registry, saveMultipliers);
    }

    public synchronized PrestigeState.RunState startRun(UUID playerUuid, PrestigeBranch branch) {
        PrestigeState state = repository.load(playerUuid);
        if (state.activeRun != null) throw new IllegalStateException("A Prestige run is already active; reset it before switching branch");
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
            run.runBranchCurrency = run.runBranchCurrency.add(BigDecimal.valueOf(run.branchLevel - oldLevel));
        }
        for (PrestigeBranchRegistry.RankDefinition rank : definition.ranks) {
            if (run.branchProgress.compareTo(rank.unlockProgress) >= 0 && !run.reachedRanks.contains(rank.rankId)) {
                run.reachedRanks.add(rank.rankId);
            }
        }
        repository.save(playerUuid, state);
    }

    public synchronized PrestigeResetService.ResetPreview previewReset(UUID playerUuid, String expectedRunId) {
        return resets.preview(playerUuid, expectedRunId);
    }

    public synchronized PrestigeResetService.ResetResult reset(UUID playerUuid, String expectedRunId) {
        return resets.reset(playerUuid, expectedRunId);
    }

    public synchronized PrestigeState snapshot(UUID playerUuid) { return repository.load(playerUuid).copy(); }

    public List<PrestigeBranch> branches() { return registry.branches(); }

    @Override
    public synchronized List<EffectResolver.Contribution> contributions(UUID playerUuid, EffectResolver.Stat stat) {
        PrestigeState state = repository.load(playerUuid);
        List<EffectResolver.Contribution> result = new ArrayList<>();

        PrestigeState.BranchState clickPermanent = state.branch(PrestigeBranch.CLICK);
        PrestigeBranchRegistry.BranchDefinition click = registry.get(PrestigeBranch.CLICK);
        if (stat == EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER && click.permanentBonusPerLevel.signum() > 0) {
            int permanentLevels = click.levelFor(clickPermanent.bestSavedProgress);
            if (permanentLevels > 0) {
                result.add(new EffectResolver.Contribution(stat,
                        click.permanentBonusPerLevel.multiply(BigDecimal.valueOf(permanentLevels)),
                        "prestige:permanent:CLICK"));
            }
        }

        PrestigeState.RunState run = state.activeRun;
        if (run == null) return Collections.unmodifiableList(result);
        PrestigeState.BranchState permanent = state.branch(run.activeBranch);
        PrestigeBranchRegistry.BranchDefinition definition = registry.get(run.activeBranch);
        for (PrestigeBranchRegistry.RankDefinition rank : definition.ranks) {
            if (rank.effectStat != stat || !run.reachedRanks.contains(rank.rankId)) continue;
            int meta = permanent.rankMetaLevels.containsKey(rank.rankId) ? permanent.rankMetaLevels.get(rank.rankId) : 0;
            result.add(new EffectResolver.Contribution(stat, rank.effectAtMetaLevel(meta),
                    "prestige:run:" + run.runId + ":rank:" + rank.rankId));
        }
        return Collections.unmodifiableList(result);
    }
}
