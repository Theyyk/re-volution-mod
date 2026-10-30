package com.example.revolution.prestige;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence-neutral Mastery state. Legacy Prestige names remain for compatibility. */
public final class PrestigeState {
    public RunState activeRun;
    public final Map<PrestigeBranch, BranchState> branches = new EnumMap<>(PrestigeBranch.class);
    public String lastResetRunId;

    public PrestigeState() {
        for (PrestigeBranch branch : PrestigeBranch.values()) branches.put(branch, new BranchState());
    }

    public BranchState branch(PrestigeBranch branch) {
        BranchState state = branches.get(branch);
        if (state == null) {
            state = new BranchState();
            branches.put(branch, state);
        }
        return state;
    }

    public PrestigeState copy() {
        PrestigeState copy = new PrestigeState();
        copy.branches.clear();
        for (Map.Entry<PrestigeBranch, BranchState> entry : branches.entrySet()) copy.branches.put(entry.getKey(), entry.getValue().copy());
        copy.activeRun = activeRun == null ? null : activeRun.copy();
        copy.lastResetRunId = lastResetRunId;
        return copy;
    }

    public static final class RunState {
        public String runId;
        public PrestigeBranch activeBranch;
        public long startedAt;
        public BigDecimal branchProgress = BigDecimal.ZERO;
        public int branchLevel;
        public final List<String> reachedRanks = new ArrayList<>();
        /** Single spendable Fragments balance. Persistence keeps the legacy field name. */
        public BigDecimal runBranchCurrency = BigDecimal.ZERO;
        public BigDecimal masteryPendingFragments = BigDecimal.ZERO;
        public BigDecimal petPendingFragments = BigDecimal.ZERO;
        public final Map<String, BigDecimal> runModifiers = new LinkedHashMap<>();

        public RunState copy() {
            RunState copy = new RunState();
            copy.runId = runId;
            copy.activeBranch = activeBranch;
            copy.startedAt = startedAt;
            copy.branchProgress = branchProgress;
            copy.branchLevel = branchLevel;
            copy.reachedRanks.addAll(reachedRanks);
            copy.runBranchCurrency = runBranchCurrency;
            copy.masteryPendingFragments = masteryPendingFragments;
            copy.petPendingFragments = petPendingFragments;
            copy.runModifiers.putAll(runModifiers);
            return copy;
        }
    }

    public static final class BranchState {
        public BigDecimal bestSavedProgress = BigDecimal.ZERO;
        public final Map<String, Integer> rankMetaLevels = new LinkedHashMap<>();
        public int rankMetaLevelCap = 5;
        public int masteryTier;
        public String masteryQuestState = "LOCKED";

        public BranchState copy() {
            BranchState copy = new BranchState();
            copy.bestSavedProgress = bestSavedProgress;
            copy.rankMetaLevels.putAll(rankMetaLevels);
            copy.rankMetaLevelCap = rankMetaLevelCap;
            copy.masteryTier = masteryTier;
            copy.masteryQuestState = masteryQuestState;
            return copy;
        }
    }
}
