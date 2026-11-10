package com.example.revolution.rune.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RuneProgressRefreshCoalescerTest {
    @Test void burstRequestsRecomputeOnceFromLatestOwnedRunes() {
        RuneProgressRefreshCoalescer coalescer = new RuneProgressRefreshCoalescer();
        List<Runnable> jobs = new ArrayList<>();
        UUID player = UUID.randomUUID();
        int[] owned = {0}, recorded = {-1};
        for (int i = 0; i < 100; i++) {
            owned[0]++;
            coalescer.request(player, jobs::add, () -> recorded[0] = owned[0]);
        }
        assertEquals(1, jobs.size());
        jobs.remove(0).run();
        assertEquals(100, recorded[0]);
        owned[0]++;
        coalescer.request(player, jobs::add, () -> recorded[0] = owned[0]);
        assertEquals(1, jobs.size());
        jobs.remove(0).run();
        assertEquals(101, recorded[0]);
    }

    @Test void independentPlayersAndNewMutationsDuringRefresh() {
        RuneProgressRefreshCoalescer coalescer = new RuneProgressRefreshCoalescer();
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID();
        List<Runnable> jobs = new ArrayList<>();
        int[] fired = {0};
        coalescer.request(p1, jobs::add, () -> {
            fired[0]++;
            coalescer.request(p1, jobs::add, () -> fired[0]++);
        });
        coalescer.request(p2, jobs::add, () -> fired[0]++);
        assertEquals(2, jobs.size());
        jobs.remove(0).run();
        assertEquals(2, jobs.size());
        jobs.remove(0).run();
        jobs.remove(0).run();
        assertEquals(3, fired[0]);
    }

    @Test void schedulingFailureReleasesPlayerForAnotherTry() {
        RuneProgressRefreshCoalescer coalescer = new RuneProgressRefreshCoalescer();
        UUID p = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> coalescer.request(
                p, runnable -> { throw new IllegalStateException("offline"); }, () -> {}));
        List<Runnable> jobs = new ArrayList<>();
        coalescer.request(p, jobs::add, () -> {});
        assertEquals(1, jobs.size());
    }
}
