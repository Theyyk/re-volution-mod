package com.example.revolution.network;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Java-only prequeue gate regressions, shared by both rune purchase handlers. */
public final class RunePurchaseRequestLimiterTest {
    private static long ms(long n) { return TimeUnit.MILLISECONDS.toNanos(n); }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    public static void main(String[] args) throws Exception {
        RunePurchaseRequestLimiter.reset();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        long t = ms(10_000);
        check(RunePurchaseRequestLimiter.tryAcquire(first, t), "first purchase accepted");
        check(!RunePurchaseRequestLimiter.tryAcquire(first, t + ms(199)), "burst before 200ms rejected");
        check(RunePurchaseRequestLimiter.tryAcquire(first, t + ms(200)), "exact 200ms boundary accepted");
        check(!RunePurchaseRequestLimiter.tryAcquire(first, t + ms(201)), "window starts from accepted click");
        check(RunePurchaseRequestLimiter.tryAcquire(second, t + ms(10)), "independent player");
        check(RunePurchaseRequestLimiter.tryAcquire(first, t + ms(400)), "rejection never extends window");
        check(!RunePurchaseRequestLimiter.tryAcquire(null, t), "invalid player rejected");
        RunePurchaseRequestLimiter.reset();
        check(RunePurchaseRequestLimiter.tryAcquire(first, t + ms(1)), "shutdown clears cooldown");

        // Concurrent network packets must have exactly one winner per window.
        RunePurchaseRequestLimiter.reset();
        final int threads = 16;
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch finish = new CountDownLatch(threads);
        final AtomicInteger accepted = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (RunePurchaseRequestLimiter.tryAcquire(first, ms(100_000))) accepted.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finish.countDown();
                }
            }).start();
        }
        ready.await(); start.countDown(); finish.await();
        check(accepted.get() == 1, "network burst must enqueue exactly one purchase");
        RunePurchaseRequestLimiter.reset();
        System.out.println("RunePurchaseRequestLimiterTest passed");
    }
}
