package com.example.revolution.quest.service;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.quest.definition.QuestState;
import com.example.revolution.quest.repository.QuestRepository;
import com.example.revolution.quest.state.QuestEntry;
import com.example.revolution.quest.state.QuestPlayerState;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative Quest engine. No client-supplied progress or reward amount is accepted. */
public final class QuestService implements GameplayEventSink {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private final QuestRepository repository;
    private final ResourceService resources;
    private final QuestGenerator generator;
    private final QuestBalanceService balance;

    public QuestService(QuestRepository repository, ResourceService resources, QuestGenerator generator, QuestBalanceService balance) {
        if (repository == null || resources == null || generator == null || balance == null) throw new IllegalArgumentException("quest dependencies are required");
        this.repository = repository;
        this.resources = resources;
        this.generator = generator;
        this.balance = balance;
    }

    public synchronized QuestPlayerState state(UUID playerUuid) {
        return normalize(playerUuid, System.currentTimeMillis()).copy();
    }

    @Override public synchronized void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) {
        if (playerUuid == null || source == null || amount == null || amount.signum() <= 0) return;
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("channel", source.name());
        apply(new GameplayEvent(playerUuid, GameplayEventType.TOTAL_DAMAGE, amount, metadata, "combat"));
        apply(new GameplayEvent(playerUuid, GameplayEventType.DAMAGE_BY_CHANNEL, amount, metadata, "combat"));
        if (source == GameplayDamageSource.CLICK) {
            apply(new GameplayEvent(playerUuid, GameplayEventType.CLICK_COUNT, BigDecimal.ONE, metadata, "combat_click"));
        }
    }

    @Override public synchronized void earnedResource(UUID playerUuid, String resource, BigDecimal amount) {
        if (playerUuid == null || resource == null || amount == null || amount.signum() <= 0) return;
        String canonical = ResourceDefinitions.normalize(resource);
        GameplayEventType type = null;
        if ("coins".equals(canonical)) type = GameplayEventType.COINS_EARNED;
        else if (ResourceDefinitions.RUNE_DUST.equals(canonical)) type = GameplayEventType.RUNE_DUST_EARNED;
        else if (ResourceDefinitions.RUNE_SHARDS.equals(canonical)) type = GameplayEventType.SHARDS_EARNED;
        if (type != null) apply(new GameplayEvent(playerUuid, type, amount, Collections.singletonMap("resource", canonical), "gameplay_earned"));
    }

    @Override public synchronized void factualEvent(GameplayEvent event) {
        if (event != null) apply(event);
    }

    public synchronized boolean claim(UUID playerUuid, String entryId) {
        long now = System.currentTimeMillis();
        QuestPlayerState state = normalize(playerUuid, now);
        QuestEntry entry = find(state, entryId);
        if (entry == null || entry.state != QuestState.COMPLETED) return false;
        if (!resources.grantPlayerResource(playerUuid, "crystals", entry.crystalReward)) return false;
        entry.state = QuestState.COOLDOWN;
        state.completedQuestCount++;
        if (state.batchCooldownUntil <= now) state.batchCooldownUntil = now + balance.cooldownMillis();
        repository.save(playerUuid, state);
        return true;
    }

    public synchronized boolean reroll(UUID playerUuid) {
        long now = System.currentTimeMillis();
        QuestPlayerState state = normalize(playerUuid, now);
        rollDay(state, now);
        if (state.rerollsUsed >= balance.dailyRerollLimit()) return false;
        List<Integer> active = new ArrayList<>();
        for (int i = 0; i < state.boardEntries.size(); i++) if (state.boardEntries.get(i).state == QuestState.ACTIVE) active.add(i);
        if (active.isEmpty()) return false;

        int difficulty = balance.difficultyTier(state.completedQuestCount);
        for (Integer index : active) {
            QuestEntry old = state.boardEntries.remove(index.intValue());
            try {
                QuestEntry replacement = generator.generate(difficulty, state.boardEntries);
                state.boardEntries.add(index, replacement);
            } catch (RuntimeException failure) {
                state.boardEntries.add(index, old);
                return false;
            }
        }
        state.rerollsUsed++;
        repository.save(playerUuid, state);
        return true;
    }

    private void apply(GameplayEvent event) {
        QuestPlayerState state = normalize(event.playerUuid, System.currentTimeMillis());
        boolean changed = false;
        for (QuestEntry entry : state.boardEntries) {
            if (entry.state != QuestState.ACTIVE || entry.eventType != event.type || !matches(entry.filters, event.metadata)) continue;
            entry.progress = entry.progress.add(event.amount).min(entry.target);
            if (entry.progress.compareTo(entry.target) >= 0) entry.state = QuestState.COMPLETED;
            changed = true;
        }
        if (changed) repository.save(event.playerUuid, state);
    }

    private QuestPlayerState normalize(UUID playerUuid, long now) {
        if (playerUuid == null) throw new IllegalArgumentException("player is required");
        QuestPlayerState state = repository.load(playerUuid);
        boolean changed = false;
        rollDay(state, now);
        if (state.batchCooldownUntil > 0L && state.batchCooldownUntil <= now) {
            int difficulty = balance.difficultyTier(state.completedQuestCount);
            for (int i = 0; i < state.boardEntries.size(); i++) {
                if (state.boardEntries.get(i).state != QuestState.COOLDOWN) continue;
                QuestEntry old = state.boardEntries.remove(i);
                try { state.boardEntries.add(i, generator.generate(difficulty, state.boardEntries)); }
                catch (RuntimeException failure) { state.boardEntries.add(i, old); }
            }
            state.batchCooldownUntil = 0L;
            changed = true;
        }
        int desired = balance.boardSize();
        int difficulty = balance.difficultyTier(state.completedQuestCount);
        while (state.boardEntries.size() < desired) {
            state.boardEntries.add(generator.generate(difficulty, state.boardEntries));
            changed = true;
        }
        if (state.boardEntries.size() > desired) {
            while (state.boardEntries.size() > desired) state.boardEntries.remove(state.boardEntries.size() - 1);
            changed = true;
        }
        if (changed) repository.save(playerUuid, state);
        sortForDisplay(state.boardEntries);
        return state;
    }

    private void rollDay(QuestPlayerState state, long now) {
        String today = DAY.format(Instant.ofEpochMilli(now));
        if (!today.equals(state.rerollDayId)) {
            state.rerollDayId = today;
            state.rerollsUsed = 0;
        }
    }

    private static boolean matches(Map<String, String> filters, Map<String, String> metadata) {
        if (filters == null || filters.isEmpty()) return true;
        if (metadata == null) return false;
        for (Map.Entry<String, String> filter : filters.entrySet()) if (!filter.getValue().equals(metadata.get(filter.getKey()))) return false;
        return true;
    }

    private static QuestEntry find(QuestPlayerState state, String entryId) {
        if (entryId == null) return null;
        for (QuestEntry entry : state.boardEntries) if (entryId.equals(entry.entryId)) return entry;
        return null;
    }

    private static void sortForDisplay(List<QuestEntry> entries) {
        Collections.sort(entries, new Comparator<QuestEntry>() {
            @Override public int compare(QuestEntry a, QuestEntry b) {
                return Integer.compare(order(a.state), order(b.state));
            }
            private int order(QuestState state) {
                if (state == QuestState.ACTIVE) return 0;
                if (state == QuestState.COMPLETED) return 1;
                return 2;
            }
        });
    }
}
