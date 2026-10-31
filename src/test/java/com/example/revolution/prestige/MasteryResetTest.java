package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public final class MasteryResetTest {
    private static final MasteryTierPolicy POLICY = policy(false, false, false, 100);
    private static final MasteryTierPolicy PREMIUM_POLICY = policy(true, true, false, 77);

    private static MasteryTierPolicy policy(boolean premiumEnabled, boolean preserveRunes, boolean preservePets, int premiumCost) {
        return tier -> {
            MasteryTierPolicy.PremiumPreservePolicy premium = new MasteryTierPolicy.PremiumPreservePolicy(
                    premiumEnabled, premiumCost * (tier + 1), preserveRunes, preservePets);
            if (tier == 0) return new MasteryTierPolicy.TierDefinition(0, 5, 1, premium);
            if (tier == 1) return new MasteryTierPolicy.TierDefinition(1, 10, 2, premium);
            if (tier == 2) return new MasteryTierPolicy.TierDefinition(2, 15, 3, premium);
            throw new IllegalArgumentException("tier is not configured");
        };
    }

    @Test
    public void eligibleMasteryResetAdvancesTierAndPreservesMetaLevels() {
        MemoryRepository repository = new MemoryRepository();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000501");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 5);
        initial.branch(PrestigeBranch.POISON).masteryTier = 0;
        repository.save(player, initial);

        PrestigeService service = service(repository, POLICY, playerUuid -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        assertTrue(service.masteryResetAvailable(player));

        PrestigeResetService.MasteryResetPreview preview = service.previewMasteryReset(player, run.runId);
        assertEquals(0, preview.currentMasteryTier);
        assertEquals(1, preview.nextMasteryTier);
        assertEquals(5, preview.currentRankMetaLevelCap);
        assertEquals(10, preview.nextRankMetaLevelCap);
        assertEquals(1, preview.currentMetaLevelGainPerRun);
        assertEquals(2, preview.nextMetaLevelGainPerRun);
        assertFalse(preview.premiumPreserveEnabled);
        assertEquals(100, preview.premiumPreserveCrystalCost);

        assertEquals(PrestigeResetService.MasteryResetResult.APPLIED, service.masteryReset(player, run.runId));
        assertEquals(PrestigeResetService.MasteryResetResult.DUPLICATE, service.masteryReset(player, run.runId));

        PrestigeState after = service.snapshot(player);
        assertNull(after.activeRun);
        assertEquals(1, after.branch(PrestigeBranch.CLICK).masteryTier);
        assertEquals(10, after.branch(PrestigeBranch.CLICK).rankMetaLevelCap);
        assertEquals(5, after.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_I"));
        assertEquals(5, after.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_II"));
        assertEquals(5, after.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_III"));
        assertEquals(0, after.branch(PrestigeBranch.POISON).masteryTier);
    }

    @Test
    public void premiumSelectionUsesBalancePolicyAndSkipsRuneResetParticipant() {
        MemoryRepository repository = new MemoryRepository();
        repository.availableCrystals = 100;
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000505");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 5);
        repository.save(player, initial);
        AtomicInteger resetParticipantCalls = new AtomicInteger();
        PrestigeService service = service(repository, PREMIUM_POLICY, ignored -> resetParticipantCalls.incrementAndGet());
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);

        PrestigeResetService.MasteryResetPreview preview = service.previewMasteryReset(player, run.runId);
        assertTrue(preview.premiumPreserveEnabled);
        assertEquals(77, preview.premiumPreserveCrystalCost);
        assertTrue(preview.premiumPreserveRunes);
        assertFalse(preview.premiumPreservePets);

        assertEquals(PrestigeResetService.MasteryResetResult.APPLIED,
                service.masteryReset(player, run.runId, MasteryResetMode.PREMIUM_PRESERVE));
        assertNotNull(repository.lastPolicy);
        assertEquals(77, repository.lastPolicy.crystalCost);
        assertTrue(repository.lastPolicy.preserveRunes);
        assertFalse(repository.lastPolicy.preservePets);
        assertEquals(23, repository.availableCrystals);
        assertEquals(0, resetParticipantCalls.get());
    }

    @Test
    public void premiumSelectionRejectsInsufficientCrystalsWithoutConsumingRun() {
        MemoryRepository repository = new MemoryRepository();
        repository.availableCrystals = 10;
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000506");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 5);
        repository.save(player, initial);
        PrestigeService service = service(repository, PREMIUM_POLICY, ignored -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);

        assertEquals(PrestigeResetService.MasteryResetResult.INSUFFICIENT_CRYSTALS,
                service.masteryReset(player, run.runId, MasteryResetMode.PREMIUM_PRESERVE));
        assertNotNull(service.activeRun(player));
        assertEquals(0, service.snapshot(player).branch(PrestigeBranch.CLICK).masteryTier);
        assertEquals(10, repository.availableCrystals);
    }

    @Test
    public void disabledPremiumSelectionIsUnavailable() {
        MemoryRepository repository = new MemoryRepository();
        repository.availableCrystals = 1000;
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000507");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 5);
        repository.save(player, initial);
        PrestigeService service = service(repository, POLICY, ignored -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);

        assertEquals(PrestigeResetService.MasteryResetResult.OPTION_UNAVAILABLE,
                service.masteryReset(player, run.runId, MasteryResetMode.PREMIUM_PRESERVE));
        assertNotNull(service.activeRun(player));
        assertNull(repository.lastPolicy);
    }

    @Test
    public void ordinaryRunsUseTierGainAndClampAtCurrentCap() {
        MemoryRepository repository = new MemoryRepository();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000502");
        PrestigeState initial = new PrestigeState();
        PrestigeState.BranchState click = initial.branch(PrestigeBranch.CLICK);
        click.masteryTier = 1;
        click.rankMetaLevelCap = 10;
        fillClickMeta(click, 9);
        repository.save(player, initial);

        PrestigeService service = service(repository, POLICY, playerUuid -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        service.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("1000"));
        PrestigeResetService.ResetPreview preview = service.previewReset(player, run.runId);
        assertEquals(2, preview.metaLevelGainPerRun);
        assertEquals(PrestigeResetService.ResetResult.APPLIED, service.reset(player, run.runId));

        PrestigeState.BranchState after = service.snapshot(player).branch(PrestigeBranch.CLICK);
        assertEquals(10, after.rankMetaLevels.get("CLICK_I"));
        assertEquals(10, after.rankMetaLevels.get("CLICK_II"));
        assertEquals(10, after.rankMetaLevels.get("CLICK_III"));
    }

    @Test
    public void readinessIsFactualAndNeverAutoResets() {
        MemoryRepository repository = new MemoryRepository();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000503");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 4);
        repository.save(player, initial);

        PrestigeService service = service(repository, POLICY, playerUuid -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        assertFalse(service.masteryResetAvailable(player));
        assertEquals(PrestigeResetService.MasteryResetResult.NOT_ELIGIBLE, service.masteryReset(player, run.runId));
        assertNotNull(service.activeRun(player));

        service.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("1000"));
        assertEquals(PrestigeResetService.ResetResult.APPLIED, service.reset(player, run.runId));
        PrestigeState afterOrdinaryReset = service.snapshot(player);
        assertEquals(0, afterOrdinaryReset.branch(PrestigeBranch.CLICK).masteryTier);
        assertEquals(5, afterOrdinaryReset.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_I"));
        assertEquals(5, afterOrdinaryReset.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_II"));
        assertEquals(5, afterOrdinaryReset.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_III"));
        assertNull(afterOrdinaryReset.activeRun);

        service.startRun(player, PrestigeBranch.CLICK);
        assertTrue(service.masteryResetAvailable(player));
        assertEquals(0, service.snapshot(player).branch(PrestigeBranch.CLICK).masteryTier);
    }

    @Test
    public void staleRunCannotIncreaseMasteryTierTwice() {
        MemoryRepository repository = new MemoryRepository();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000504");
        PrestigeState initial = new PrestigeState();
        fillClickMeta(initial.branch(PrestigeBranch.CLICK), 5);
        repository.save(player, initial);

        PrestigeService service = service(repository, POLICY, playerUuid -> { });
        PrestigeState.RunState run = service.startRun(player, PrestigeBranch.CLICK);
        assertEquals(PrestigeResetService.MasteryResetResult.APPLIED, service.masteryReset(player, run.runId));
        assertEquals(PrestigeResetService.MasteryResetResult.DUPLICATE, service.masteryReset(player, run.runId));
        assertEquals(1, service.snapshot(player).branch(PrestigeBranch.CLICK).masteryTier);
    }

    private static PrestigeService service(MemoryRepository repository, MasteryTierPolicy policy,
                                           PrestigeService.AppliedResetParticipant participant) {
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        return new PrestigeService(repository, registry,
                (player, branch) -> BigDecimal.ONE,
                participant,
                policy);
    }

    private static void fillClickMeta(PrestigeState.BranchState branch, int value) {
        branch.rankMetaLevels.put("CLICK_I", value);
        branch.rankMetaLevels.put("CLICK_II", value);
        branch.rankMetaLevels.put("CLICK_III", value);
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        private int availableCrystals = Integer.MAX_VALUE;
        private ResetCommitPolicy lastPolicy;

        @Override public PrestigeState load(UUID playerUuid) {
            PrestigeState state = states.get(playerUuid);
            return state == null ? new PrestigeState() : state.copy();
        }

        @Override public void save(UUID playerUuid, PrestigeState state) {
            states.put(playerUuid, state.copy());
        }

        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state) {
            return commitReset(playerUuid, expectedRunId, state, ResetCommitPolicy.normal());
        }

        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state, ResetCommitPolicy policy) {
            PrestigeState current = load(playerUuid);
            if (current.activeRun == null) {
                return expectedRunId != null && expectedRunId.equals(current.lastResetRunId)
                        ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            }
            if (expectedRunId == null || !expectedRunId.equals(current.activeRun.runId)) return ResetCommitResult.STALE;
            if (availableCrystals < policy.crystalCost) return ResetCommitResult.INSUFFICIENT_CRYSTALS;
            availableCrystals -= policy.crystalCost;
            lastPolicy = policy;
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
