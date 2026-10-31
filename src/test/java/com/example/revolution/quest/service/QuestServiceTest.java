package com.example.revolution.quest.service;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.quest.definition.QuestState;
import com.example.revolution.quest.definition.QuestTemplate;
import com.example.revolution.quest.repository.QuestRepository;
import com.example.revolution.quest.state.QuestEntry;
import com.example.revolution.quest.state.QuestPlayerState;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestServiceTest {
    @Test public void initialStateCreatesThreeUniqueActiveQuests() {
        Fixture f = new Fixture();
        QuestPlayerState state = f.quests.state(f.id);
        assertEquals(3, state.boardEntries.size());
        Set<String> variants = new HashSet<>();
        for (QuestEntry entry : state.boardEntries) {
            assertEquals(QuestState.ACTIVE, entry.state);
            assertTrue(variants.add(entry.variantKey()));
            assertEquals(0, entry.difficultyTier);
        }
        assertEquals(0L, state.completedQuestCount);
        assertEquals(0L, state.batchCooldownUntil);
    }

    @Test public void disabledTemplateIsNeverEligibleForGeneration() {
        Fixture f = new Fixture();
        boolean foundBoss = false;
        for (QuestTemplate template : new QuestCatalog(f.questBalance).templates()) {
            if (!"boss_kill".equals(template.id)) continue;
            foundBoss = true;
            assertFalse(template.enabled);
            assertFalse(template.eligible(8));
        }
        assertTrue(foundBoss);
    }

    @Test public void clickDamageCountsOneClickInsteadOfDamageAmount() {
        Fixture f = new Fixture();
        QuestEntry clicks = active("clicks");
        clicks.target = BigDecimal.valueOf(2);
        QuestEntry unrelated = active("other");
        unrelated.eventType = GameplayEventType.RUNE_PURCHASE_ANY;
        f.questRepo.state = board(clicks, unrelated, completed("completed"));

        f.quests.confirmedDamage(f.id, GameplayDamageSource.CLICK, new BigDecimal("999999"));
        assertDecimal("1", find(f.questRepo.state, "clicks").progress);
        f.quests.confirmedDamage(f.id, GameplayDamageSource.POISON, new BigDecimal("500"));
        assertDecimal("1", find(f.questRepo.state, "clicks").progress);
        f.quests.confirmedDamage(f.id, GameplayDamageSource.CLICK, BigDecimal.ONE);
        QuestEntry completedClick = find(f.questRepo.state, "clicks");
        assertDecimal("2", completedClick.progress);
        assertEquals(QuestState.COMPLETED, completedClick.state);
    }

    @Test public void claimIsManualOneShotAndStartsSharedBatch() {
        Fixture f = new Fixture();
        QuestPlayerState stored = board(completed("a"), completed("b"), active("c"));
        f.questRepo.state = stored;

        assertTrue(f.quests.claim(f.id, "a"));
        QuestPlayerState afterFirst = f.questRepo.state.copy();
        long deadline = afterFirst.batchCooldownUntil;
        assertTrue(deadline > System.currentTimeMillis());
        assertEquals(1L, afterFirst.completedQuestCount);
        assertDecimal("2", f.resourceRepo.getPlayerResourceValue(f.id, "crystals"));
        assertFalse(f.quests.claim(f.id, "a"));
        assertDecimal("2", f.resourceRepo.getPlayerResourceValue(f.id, "crystals"));
        assertEquals(1L, f.questRepo.state.completedQuestCount);

        assertTrue(f.quests.claim(f.id, "b"));
        assertEquals(deadline, f.questRepo.state.batchCooldownUntil);
        assertEquals(2L, f.questRepo.state.completedQuestCount);
        assertDecimal("4", f.resourceRepo.getPlayerResourceValue(f.id, "crystals"));
    }

    @Test public void expiredBatchReplacesOnlyCooldownEntries() {
        Fixture f = new Fixture();
        QuestEntry cooldown = active("cooldown-old"); cooldown.state = QuestState.COOLDOWN;
        QuestEntry completed = completed("completed-stays");
        QuestEntry active = active("active-stays"); active.progress = new BigDecimal("3");
        QuestPlayerState stored = board(cooldown, completed, active);
        stored.batchCooldownUntil = System.currentTimeMillis() - 1L;
        f.questRepo.state = stored;

        QuestPlayerState refreshed = f.quests.state(f.id);
        assertEquals(3, refreshed.boardEntries.size());
        assertNotNull(find(refreshed, "completed-stays"));
        assertNotNull(find(refreshed, "active-stays"));
        assertEquals(new BigDecimal("3"), find(refreshed, "active-stays").progress);
        assertNull(find(refreshed, "cooldown-old"));
        assertEquals(0L, refreshed.batchCooldownUntil);
    }

    @Test public void rerollTouchesOnlyActiveAndConsumesOneUse() {
        Fixture f = new Fixture();
        QuestEntry completed = completed("completed-stays");
        QuestEntry cooldown = active("cooldown-stays"); cooldown.state = QuestState.COOLDOWN;
        QuestEntry active = active("active-old");
        QuestPlayerState stored = board(active, completed, cooldown);
        stored.batchCooldownUntil = System.currentTimeMillis() + 60_000L;
        f.questRepo.state = stored;

        assertTrue(f.quests.reroll(f.id));
        QuestPlayerState after = f.questRepo.state;
        assertEquals(1, after.rerollsUsed);
        assertNotNull(find(after, "completed-stays"));
        assertNotNull(find(after, "cooldown-stays"));
        assertNull(find(after, "active-old"));
    }

    private static QuestPlayerState board(QuestEntry a, QuestEntry b, QuestEntry c) {
        QuestPlayerState state = new QuestPlayerState();
        state.boardEntries.add(a); state.boardEntries.add(b); state.boardEntries.add(c);
        return state;
    }

    private static QuestEntry active(String id) {
        QuestEntry entry = new QuestEntry();
        entry.entryId = id;
        entry.templateId = "manual-" + id;
        entry.eventType = GameplayEventType.CLICK_COUNT;
        entry.target = BigDecimal.TEN;
        entry.crystalReward = 2;
        return entry;
    }

    private static QuestEntry completed(String id) {
        QuestEntry entry = active(id);
        entry.progress = entry.target;
        entry.state = QuestState.COMPLETED;
        return entry;
    }

    private static QuestEntry find(QuestPlayerState state, String id) {
        for (QuestEntry entry : state.boardEntries) if (id.equals(entry.entryId)) return entry;
        return null;
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final MemoryQuestRepository questRepo = new MemoryQuestRepository();
        final MemoryResourceRepository resourceRepo = new MemoryResourceRepository();
        final ResourceService resources = new ResourceService(resourceRepo);
        final BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        final QuestBalanceService questBalance = new QuestBalanceService(balance);
        final QuestService quests = new QuestService(questRepo, resources,
                new QuestGenerator(new QuestCatalog(questBalance)), questBalance);
    }

    private static final class MemoryQuestRepository implements QuestRepository {
        QuestPlayerState state = new QuestPlayerState();
        @Override public QuestPlayerState load(UUID playerUuid) { return state.copy(); }
        @Override public void save(UUID playerUuid, QuestPlayerState state) { this.state = state.copy(); }
    }

    private static final class MemoryResourceRepository implements ResourceRepository {
        final Map<String, BigDecimal> values = new HashMap<>();
        @Override public void purgeDeprecatedLightnings() { }
        @Override public void purgeDeprecatedLightnings(UUID uuid) { }
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            BigDecimal value = values.get(uuid + ":" + resource);
            return value == null ? BigDecimal.ZERO : value;
        }
        @Override public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
            values.put(uuid + ":" + canonical, value);
        }
    }
}
