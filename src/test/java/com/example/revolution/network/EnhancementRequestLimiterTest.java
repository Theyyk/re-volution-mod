package com.example.revolution.network;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Verifies the per-player 100 ms enhancement request window without Minecraft runtime. */
public final class EnhancementRequestLimiterTest {
    public static void main(String[] args) {
        EnhancementRequestLimiter limiter = new EnhancementRequestLimiter();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        long start = TimeUnit.SECONDS.toNanos(10L);

        check(limiter.tryAcquire(first, start), "first request must be accepted");
        check(!limiter.tryAcquire(first, start + TimeUnit.MILLISECONDS.toNanos(99L)),
                "request inside 100 ms window must be rejected");
        check(limiter.tryAcquire(first, start + TimeUnit.MILLISECONDS.toNanos(100L)),
                "request at 100 ms boundary must be accepted");
        check(limiter.tryAcquire(second, start + TimeUnit.MILLISECONDS.toNanos(50L)),
                "different players must have independent cooldowns");
        check(!limiter.tryAcquire(first, start + TimeUnit.MILLISECONDS.toNanos(150L)),
                "accepted request must start the next cooldown window");
        check(limiter.tryAcquire(first, start + TimeUnit.MILLISECONDS.toNanos(200L)),
                "rejected requests must not extend the cooldown window");

        System.out.println("EnhancementRequestLimiterTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
