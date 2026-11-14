package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contract tests for already implemented Mastery rewards and rank effects; no new balance rules. */
public final class MasteryRunRewardLifecycleRegressionTest {
    @Test public void duplicateCollectAndStaleRunCannotGrantAdditionalFragments() {
        MemoryRepo repo = new MemoryRepo();
        PrestigeService mastery = new PrestigeService(repo, new PrestigeBranchRegistry());
        UUID player = UUID.randomUUID();
        String runId = mastery.startRun(player, PrestigeBranch.CLICK).runId;
        mastery.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("500"));

        BigDecimal pending = mastery.activeRun(player).masteryPendingFragments;
        assertTrue(pending.signum() > 0);
        assertEquals(0, pending.compareTo(mastery.collectFragments(player, runId, PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.collectFragments(player, runId, PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.collectFragments(player, UUID.randomUUID().toString(), PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, pending.compareTo(mastery.activeRun(player).runBranchCurrency));
    }

    @Test public void reconnectRecomputesRankEffectsWithoutStackingAndKeepsRewards() {
        MemoryRepo repo = new MemoryRepo();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService before = new PrestigeService(repo, registry);
        UUID player = UUID.randomUUID();
        String runId = before.startRun(player, PrestigeBranch.CLICK).runId;
        before.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("500"));
        BigDecimal pendingBefore = before.activeRun(player).masteryPendingFragments;

        EffectResolver firstResolver = new EffectResolver();
        firstResolver.register(before);
        BigDecimal first = firstResolver.multiplier(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER);

        PrestigeService after = new PrestigeService(repo, registry);
        EffectResolver reconnectResolver = new EffectResolver();
        reconnectResolver.register(after);
        assertEquals(runId, after.activeRun(player).runId);
        assertEquals(0, pendingBefore.compareTo(after.activeRun(player).masteryPendingFragments));
        assertEquals(0, first.compareTo(reconnectResolver.multiplier(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)));
        assertEquals(0, pendingBefore.compareTo(after.collectFragments(player, runId, PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(before.collectFragments(player, runId, PrestigeService.FragmentSource.MASTERY)));
    }

    @Test public void rewardsAreIsolatedBetweenPlayersAndBranches() {
        MemoryRepo repo = new MemoryRepo();
        PrestigeService mastery = new PrestigeService(repo, new PrestigeBranchRegistry());
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        String aRun = mastery.startRun(a, PrestigeBranch.CLICK).runId;
        String bRun = mastery.startRun(b, PrestigeBranch.POISON).runId;
        mastery.recordProgress(a, PrestigeBranch.CLICK, new BigDecimal("500"));
        mastery.recordProgress(b, PrestigeBranch.CLICK, new BigDecimal("500"));
        assertTrue(mastery.activeRun(a).masteryPendingFragments.signum() > 0);
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.activeRun(b).masteryPendingFragments));
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.collectFragments(b, aRun, PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.collectFragments(a, bRun, PrestigeService.FragmentSource.MASTERY)));
        assertEquals(0, BigDecimal.ZERO.compareTo(mastery.activeRun(b).runBranchCurrency));
    }

    private static final class MemoryRepo implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        @Override public PrestigeState load(UUID id) {
            PrestigeState state = states.get(id);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID id, PrestigeState state) { states.put(id, state.copy()); }
        @Override public ResetCommitResult commitReset(UUID id, String runId, PrestigeState next) {
            PrestigeState previous = load(id);
            if (previous.activeRun == null)
                return runId != null && runId.equals(previous.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            if (!previous.activeRun.runId.equals(runId)) return ResetCommitResult.STALE;
            states.put(id, next.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
