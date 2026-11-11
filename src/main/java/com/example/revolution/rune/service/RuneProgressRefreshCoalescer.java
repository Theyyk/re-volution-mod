package com.example.revolution.rune.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Deduplicates pending derived mechanic-progress refreshes per player.
 * Purchases, ownership writes, charges, unlock checks and gameplay events
 * continue synchronously and are never queued or dropped by this helper.
 */
public final class RuneProgressRefreshCoalescer {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public void request(UUID player, Consumer<Runnable> scheduler, Runnable refresh) {
        if (player == null || scheduler == null || refresh == null) return;
        if (!pending.add(player)) return;
        try {
            scheduler.accept(() -> {
                pending.remove(player);
                refresh.run();
            });
        } catch (RuntimeException failure) {
            pending.remove(player);
            throw failure;
        }
    }
}
