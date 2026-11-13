package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.persistence.MongoPrestigeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public final class PrestigeFoundationTest {
    @Test
    public void clickRunProgressRanksEffectsAndSafeReset() {
        MemoryRepository repository = new MemoryRepository();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService prestige = new PrestigeService(repository, registry);
        EffectResolver effects = new EffectResolver();
        effects.register(prestige);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000101");

        PrestigeState.RunState first = prestige.startRun(player, PrestigeBranch.CLICK);
        prestige.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("600"));
        PrestigeState running = prestige.snapshot(player);
        assertEquals(2, running.activeRun.branchLevel);
        assertEquals(Arrays.asList("CLICK_I", "CLICK_II"), running.activeRun.reachedRanks);
        assertEquals(0, BigDecimal.ZERO.compareTo(running.activeRun.runBranchCurrency));
        assertEquals(0, new BigDecimal("2").compareTo(running.activeRun.masteryPendingFragments));
        assertEquals(0, BigDecimal.ZERO.compareTo(running.activeRun.petPendingFragments));
        assertEquals(0, effects.multiplier(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)
                .compareTo(new BigDecimal("1.15")));

        PrestigeResetService.ResetPreview preview = prestige.previewReset(player, first.runId);
        assertEquals(0, preview.currentResult.compareTo(new BigDecimal("600")));
        assertEquals(0, preview.newBest.compareTo(new BigDecimal("600")));
        assertEquals(2, preview.ranksUpgraded.size());
        assertEquals(PrestigeResetService.ResetResult.APPLIED, prestige.reset(player, first.runId));
        assertEquals(PrestigeResetService.ResetResult.DUPLICATE, prestige.reset(player, first.runId));

        PrestigeState after = prestige.snapshot(player);
        assertNull(after.activeRun);
        assertEquals(0, after.branch(PrestigeBranch.CLICK).bestSavedProgress.compareTo(new BigDecimal("600")));
        assertEquals(1, after.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_I"));
        assertEquals(1, after.branch(PrestigeBranch.CLICK).rankMetaLevels.get("CLICK_II"));
        assertEquals(0, effects.multiplier(player, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER)
                .compareTo(new BigDecimal("1.02")));

        assertTrue(prestige.completeLocationReturn(player, first.runId));
        PrestigeState.RunState weaker = prestige.startRun(player, PrestigeBranch.CLICK);
        prestige.recordProgress(player, PrestigeBranch.CLICK, new BigDecimal("200"));
        assertEquals(PrestigeResetService.ResetResult.STALE, prestige.reset(player, first.runId));
        assertEquals(PrestigeResetService.ResetResult.APPLIED, prestige.reset(player, weaker.runId));
        assertEquals(0, prestige.snapshot(player).branch(PrestigeBranch.CLICK).bestSavedProgress.compareTo(new BigDecimal("600")));
    }

    @Test
    public void branchSwitchRequiresNewRun() {
        MemoryRepository repository = new MemoryRepository();
        PrestigeService prestige = new PrestigeService(repository, new PrestigeBranchRegistry());
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000102");
        prestige.startRun(player, PrestigeBranch.CLICK);
        boolean rejected = false;
        try { prestige.startRun(player, PrestigeBranch.POISON); }
        catch (IllegalStateException expected) { rejected = true; }
        assertTrue(rejected);
    }

    @Test
    public void resetRemovesOrdinaryAndImprovedButPreservesAdvancedAndCrystals() {
        Document ordinary = new Document("slot", 1).append("cardIndex", 1).append("layer", 1);
        Document improved = new Document("slot", 2).append("cardIndex", 2).append("layer", 2);
        Document advanced = new Document("slot", 3).append("cardIndex", 3).append("layer", 3);
        Document enhancements = new Document("1", 44).append("2", 55).append("3", 66);
        Document collection = new Document("name", "A")
                .append("cards", new ArrayList<>(Arrays.asList(ordinary, improved, advanced)))
                .append("runeEnhancements", enhancements);
        Document player = new Document("crystals", 777).append("decks", new ArrayList<>(Arrays.asList(collection)));
        Document set = new Document("prestige", new Document("lastResetRunId", "r1"));

        MongoPrestigeRepository.appendRuneOwnershipWipe(player, set);

        @SuppressWarnings("unchecked")
        List<Document> collections = (List<Document>) set.get("decks");
        assertNotNull(collections);
        @SuppressWarnings("unchecked")
        List<Document> runes = (List<Document>) collections.get(0).get("cards");
        assertEquals(1, runes.size());
        assertEquals(3, runes.get(0).getInteger("layer").intValue());
        assertEquals(1, collections.get(0).get("runeEnhancements", Document.class).size());
        assertEquals(66, collections.get(0).get("runeEnhancements", Document.class).getInteger("3").intValue());
        assertEquals("A", collections.get(0).getString("name"));
        assertEquals(777, player.getInteger("crystals"));
        assertEquals(3, ((List<?>) collection.get("cards")).size());
        assertFalse(set.containsKey("crystals"));
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();

        @Override public PrestigeState load(UUID playerUuid) {
            PrestigeState state = states.get(playerUuid);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID playerUuid, PrestigeState state) { states.put(playerUuid, state.copy()); }
        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state) {
            PrestigeState current = load(playerUuid);
            if (current.activeRun == null) {
                return expectedRunId != null && expectedRunId.equals(current.lastResetRunId) ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            }
            if (!current.activeRun.runId.equals(expectedRunId)) return ResetCommitResult.STALE;
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
