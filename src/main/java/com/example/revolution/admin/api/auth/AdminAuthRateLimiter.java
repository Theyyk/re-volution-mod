package com.example.revolution.admin.api.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/** Small bounded sliding-window limiter used only around authentication endpoints. */
final class AdminAuthRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Deque<Long> accepted = new ArrayDeque<>();

    AdminAuthRateLimiter(int limit, long windowMillis, LongSupplier clock) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    synchronized boolean tryAcquire() {
        long now = clock.getAsLong();
        while (!accepted.isEmpty() && now - accepted.peekFirst() >= windowMillis) accepted.removeFirst();
        if (accepted.size() >= limit) return false;
        accepted.addLast(now);
        return true;
    }
}
