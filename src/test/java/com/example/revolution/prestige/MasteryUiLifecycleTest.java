package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Test the canonical engine path used by the Mastery GUI, without command-level shortcuts. */
public final class MasteryUiLifecycleTest {
    @Test public void startAndDuplicateDoNotCreateSecondRun() {
        MemoryRepository repo = new MemoryRepository();
        PrestigeService service = new PrestigeService(repo, new PrestigeBranchRegistry());
        UUID player = UUID.randomUUID();
        assertNull(service.activeRun(player));
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        assertNotNull(run.runId);
        assertEquals(run.runId, service.activeRun(player).runId);
        assertThrows(IllegalStateException.class, () -> service.startRun(player, PrestigeBranch.COINS));
        assertEquals(run.runId, service.activeRun(player).runId);
    }

    @Test public void persistentRunAndTwoPlayersRemainIndependent() {
        MemoryRepository repo = new MemoryRepository();
        PrestigeService service = new PrestigeService(repo, new PrestigeBranchRegistry());
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        PrestigeState.RunState a = service.startRun(first, PrestigeBranch.POISON);
        PrestigeState.RunState b = service.startRun(second, PrestigeBranch.COINS);
        service.recordProgress(first, PrestigeBranch.POISON, new BigDecimal("100"));
        assertEquals(new BigDecimal("100"), service.snapshot(first).activeRun.branchProgress);
        assertEquals(BigDecimal.ZERO, service.snapshot(second).activeRun.branchProgress);
        PrestigeService restarted = new PrestigeService(repo, new PrestigeBranchRegistry());
        assertEquals(a.runId, restarted.activeRun(first).runId);
        assertEquals(b.runId, restarted.activeRun(second).runId);
        assertEquals(PrestigeResetService.ResetResult.APPLIED, restarted.reset(first, a.runId));
        assertNull(restarted.activeRun(first));
        assertNotNull(restarted.activeRun(second));
        assertEquals(PrestigeResetService.ResetResult.DUPLICATE, restarted.reset(first, a.runId));
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        @Override public PrestigeState load(UUID uuid) {
            PrestigeState state = states.get(uuid);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID uuid, PrestigeState state) {
            states.put(uuid, state.copy());
        }
        @Override public ResetCommitResult commitReset(UUID uuid, String expectedRunId, PrestigeState next) {
            PrestigeState before = load(uuid);
            if (before.activeRun == null)
                return expectedRunId.equals(before.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            if (!expectedRunId.equals(before.activeRun.runId)) return ResetCommitResult.STALE;
            save(uuid, next);
            return ResetCommitResult.APPLIED;
        }
    }
}
