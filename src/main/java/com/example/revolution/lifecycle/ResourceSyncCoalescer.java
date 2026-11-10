package com.example.revolution.lifecycle;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Collapses pending resource UI refreshes for the same player into one latest-state
 * refresh. This never coalesces resource writes or gameplay purchase requests.
 */
public final class ResourceSyncCoalescer {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public void request(UUID player, Consumer<Runnable> scheduler, Runnable sync) {
        if (player == null || scheduler == null || sync == null) return;
        if (!pending.add(player)) return;
        try {
            scheduler.accept(() -> {
                // Remove before sync: a concurrent mutation while the snapshot is
                // being built can schedule a fresh refresh rather than be lost.
                pending.remove(player);
                sync.run();
            });
        } catch (RuntimeException error) {
            pending.remove(player);
            throw error;
        }
    }

    public void clear() { pending.clear(); }
}
