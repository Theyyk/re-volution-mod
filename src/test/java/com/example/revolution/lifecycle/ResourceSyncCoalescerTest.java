package com.example.revolution.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ResourceSyncCoalescerTest {
    @Test void burstWritesScheduleOnlyOneAuthoritativeRefreshPerPlayer() {
        ResourceSyncCoalescer coalescer = new ResourceSyncCoalescer();
        UUID player = UUID.randomUUID();
        List<Runnable> tasks = new ArrayList<>();
        int[] wallet = {0}, observed = {-1};
        for (int i = 0; i < 100; i++) {
            wallet[0]++;
            coalescer.request(player, tasks::add, () -> observed[0] = wallet[0]);
        }
        assertEquals(1, tasks.size(), "only UI refresh is coalesced, not writes");
        tasks.remove(0).run();
        assertEquals(100, observed[0], "refresh reads the latest authoritative wallet");
        wallet[0]++;
        coalescer.request(player, tasks::add, () -> observed[0] = wallet[0]);
        assertEquals(1, tasks.size(), "later mutations still publish updates");
        tasks.remove(0).run();
        assertEquals(101, observed[0]);
    }

    @Test void playersAreIndependentAndFailuresDoNotStrandPendingSet() {
        ResourceSyncCoalescer coalescer = new ResourceSyncCoalescer();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        List<Runnable> tasks = new ArrayList<>();
        int[] seen = {0};
        coalescer.request(first, tasks::add, () -> seen[0]++);
        coalescer.request(second, tasks::add, () -> seen[0]++);
        coalescer.request(first, tasks::add, () -> seen[0]++);
        assertEquals(2, tasks.size());
        for (Runnable task : tasks) task.run();
        assertEquals(2, seen[0]);
        assertThrows(IllegalStateException.class, () ->
                coalescer.request(first, task -> { throw new IllegalStateException("scheduler offline"); },
                        () -> seen[0]++));
        tasks.clear();
        coalescer.request(first, tasks::add, () -> seen[0]++);
        assertEquals(1, tasks.size(), "failed scheduling must release pending lease");
    }

    @Test void mutationDuringRefreshSchedulesAnotherUpdate() {
        ResourceSyncCoalescer coalescer = new ResourceSyncCoalescer();
        UUID player = UUID.randomUUID();
        List<Runnable> tasks = new ArrayList<>();
        int[] seen = {0};
        coalescer.request(player, tasks::add, () -> {
            seen[0]++;
            coalescer.request(player, tasks::add, () -> seen[0]++);
        });
        tasks.remove(0).run();
        assertEquals(1, tasks.size());
        tasks.remove(0).run();
        assertEquals(2, seen[0]);
    }
}
