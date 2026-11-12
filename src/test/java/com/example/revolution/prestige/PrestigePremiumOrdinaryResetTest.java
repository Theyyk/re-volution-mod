package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Ordinary premium reset uses the same Balance Core contract and runId CAS. */
public final class PrestigePremiumOrdinaryResetTest {
    @Test public void ordinaryPremiumPreservesRunesAndCommitsOnce() {
        MemoryRepository repo = new MemoryRepository();
        MasteryTierPolicy tierPolicy = tier -> new MasteryTierPolicy.TierDefinition(tier, 5, 1,
                new MasteryTierPolicy.PremiumPreservePolicy(true, 25, true, true));
        AtomicInteger runeResetObserver = new AtomicInteger();
        PrestigeService service = new PrestigeService(repo, new PrestigeBranchRegistry(),
                (uuid, direction) -> BigDecimal.ONE, uuid -> runeResetObserver.incrementAndGet(), tierPolicy);
        UUID player = UUID.randomUUID();
        String run = service.startRun(player, PrestigeBranch.CLICK).runId;
        assertTrue(service.premiumPreservePolicy(player, run).enabled);
        assertEquals(PrestigeResetService.ResetResult.APPLIED, service.reset(player, run, true));
        assertEquals(25, repo.lastPolicy.crystalCost);
        assertTrue(repo.lastPolicy.preserveRunes);
        assertTrue(repo.lastPolicy.preservePets);
        assertEquals(0, runeResetObserver.get());
        assertEquals(PrestigeResetService.ResetResult.DUPLICATE, service.reset(player, run, true));
        assertEquals(1, repo.commits);
    }

    @Test public void ordinaryResetWithoutPremiumKeepsLegacyBehaviour() {
        MemoryRepository repo = new MemoryRepository();
        MasteryTierPolicy policy = tier -> new MasteryTierPolicy.TierDefinition(tier, 5, 1,
                new MasteryTierPolicy.PremiumPreservePolicy(false, 25, true, true));
        AtomicInteger runeResetObserver = new AtomicInteger();
        PrestigeService service = new PrestigeService(repo, new PrestigeBranchRegistry(),
                (uuid, direction) -> BigDecimal.ONE, uuid -> runeResetObserver.incrementAndGet(), policy);
        UUID player = UUID.randomUUID();
        String run = service.startRun(player, PrestigeBranch.POISON).runId;
        assertEquals(PrestigeResetService.ResetResult.OPTION_UNAVAILABLE, service.reset(player, run, true));
        assertNotNull(service.activeRun(player));
        assertEquals(PrestigeResetService.ResetResult.APPLIED, service.reset(player, run));
        assertFalse(repo.lastPolicy.preserveRunes);
        assertEquals(1, runeResetObserver.get());
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        PrestigeRepository.ResetCommitPolicy lastPolicy;
        int commits;
        @Override public PrestigeState load(UUID player) {
            PrestigeState state = states.get(player);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID player, PrestigeState state) { states.put(player, state.copy()); }
        @Override public ResetCommitResult commitReset(UUID player, String runId, PrestigeState state) {
            return commitReset(player, runId, state, PrestigeRepository.ResetCommitPolicy.normal());
        }
        @Override public ResetCommitResult commitReset(UUID player, String runId, PrestigeState state,
                                                        PrestigeRepository.ResetCommitPolicy policy) {
            PrestigeState old = load(player);
            if (old.activeRun == null)
                return runId.equals(old.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            if (!runId.equals(old.activeRun.runId)) return ResetCommitResult.STALE;
            lastPolicy = policy; commits++; save(player, state);
            return ResetCommitResult.APPLIED;
        }
    }
}
