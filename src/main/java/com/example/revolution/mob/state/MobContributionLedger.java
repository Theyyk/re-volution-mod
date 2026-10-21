package com.example.revolution.mob.state;

import com.example.revolution.number.LargeNumbers;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-physical-mob runtime contribution ledger. It is intentionally in-memory only:
 * a respawn or unload starts a fresh combat attempt.
 */
public final class MobContributionLedger {
    private static final class Entry {
        BigDecimal amount = BigDecimal.ZERO;
        long reachedOrder;
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private long sequence;

    public void add(UUID playerUuid, BigDecimal damage) {
        if (playerUuid == null || damage == null || damage.signum() <= 0) return;
        Entry entry = entries.computeIfAbsent(playerUuid, ignored -> new Entry());
        entry.amount = LargeNumbers.normalize(entry.amount.add(damage));
        entry.reachedOrder = ++sequence;
    }

    public BigDecimal remove(UUID playerUuid) {
        if (playerUuid == null) return BigDecimal.ZERO;
        Entry removed = entries.remove(playerUuid);
        return removed == null ? BigDecimal.ZERO : removed.amount;
    }

    public BigDecimal amount(UUID playerUuid) {
        Entry entry = entries.get(playerUuid);
        return entry == null ? BigDecimal.ZERO : entry.amount;
    }

    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (Entry entry : entries.values()) total = total.add(entry.amount);
        return LargeNumbers.normalize(total);
    }

    public UUID leadingContributor() {
        UUID bestUuid = null;
        BigDecimal bestAmount = BigDecimal.ZERO;
        long bestReachedOrder = Long.MAX_VALUE;
        for (Map.Entry<UUID, Entry> item : entries.entrySet()) {
            Entry entry = item.getValue();
            int amountCompare = entry.amount.compareTo(bestAmount);
            if (bestUuid == null || amountCompare > 0
                    || (amountCompare == 0 && entry.reachedOrder < bestReachedOrder)) {
                bestUuid = item.getKey();
                bestAmount = entry.amount;
                bestReachedOrder = entry.reachedOrder;
            }
        }
        return bestUuid;
    }

    public Set<UUID> participants() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(entries.keySet()));
    }

    public Map<UUID, BigDecimal> snapshot() {
        Map<UUID, BigDecimal> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, Entry> item : entries.entrySet()) copy.put(item.getKey(), item.getValue().amount);
        return Collections.unmodifiableMap(copy);
    }

    public boolean isEmpty() { return entries.isEmpty(); }

    public void clear() {
        entries.clear();
        sequence = 0L;
    }
}
