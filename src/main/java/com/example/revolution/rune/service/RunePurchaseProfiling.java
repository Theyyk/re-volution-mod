package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * DEV diagnostic only: measures queued purchase delay, server-thread work and
 * synchronous Mongo operations without changing purchase order or outcomes.
 * No player identifiers, balances, or rune rolls are logged.
 */
public final class RunePurchaseProfiling {
    private static final long NANOS_PER_MS = 1000000L;
    private static final String ORDINARY = "ordinary";
    private static final String CRYSTAL = "crystal";
    private static final AtomicInteger ordinaryPending = new AtomicInteger();
    private static final AtomicInteger crystalPending = new AtomicInteger();
    private static final ThreadLocal<Sample> ACTIVE = new ThreadLocal<>();
    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<>();

    private RunePurchaseProfiling() {}

    public static void queued(String path) { pending(path).incrementAndGet(); }

    public static void process(String path, long enqueuedAt, Runnable task) {
        long start = System.nanoTime();
        Sample previous = ACTIVE.get();
        Sample sample = new Sample();
        ACTIVE.set(sample);
        try {
            task.run();
        } finally {
            long elapsed = System.nanoTime() - start;
            ACTIVE.remove();
            if (previous != null) ACTIVE.set(previous);
            int remaining = pending(path).decrementAndGet();
            long queued = Math.max(0L, start - enqueuedAt);
            WINDOWS.computeIfAbsent(path, key -> new Window(key))
                    .record(queued, elapsed, sample, Math.max(0, remaining));
        }
    }

    public static <T> T mongoRead(Supplier<T> action) {
        Sample sample = ACTIVE.get();
        if (sample == null) return action.get();
        long started = System.nanoTime();
        try { return action.get(); }
        finally { sample.readNanos += System.nanoTime() - started; sample.reads++; }
    }

    public static void mongoWrite(Runnable action) {
        Sample sample = ACTIVE.get();
        if (sample == null) { action.run(); return; }
        long started = System.nanoTime();
        try { action.run(); }
        finally { sample.writeNanos += System.nanoTime() - started; sample.writes++; }
    }

    private static AtomicInteger pending(String path) {
        return CRYSTAL.equals(path) ? crystalPending : ordinaryPending;
    }

    private static long millis(long nanos) { return Math.max(0L, nanos) / NANOS_PER_MS; }

    private static final class Sample {
        long readNanos, writeNanos;
        int reads, writes;
    }

    private static final class Window {
        final String path;
        int count, maxPending, reads, writes;
        long totalQueue, totalExecution, totalMongoRead, totalMongoWrite;
        long maxQueue, maxExecution, lastSlowWarningNanos;

        Window(String path) { this.path = path; }

        synchronized void record(long queueNanos, long executionNanos, Sample sample, int pending) {
            count++;
            totalQueue += queueNanos;
            totalExecution += executionNanos;
            totalMongoRead += sample.readNanos;
            totalMongoWrite += sample.writeNanos;
            reads += sample.reads;
            writes += sample.writes;
            maxQueue = Math.max(maxQueue, queueNanos);
            maxExecution = Math.max(maxExecution, executionNanos);
            maxPending = Math.max(maxPending, pending);

            // A single slow purchase must be observable even if fewer than 25 are made.
            long now = System.nanoTime();
            if (millis(executionNanos) >= 150L && now - lastSlowWarningNanos >= 1000000000L) {
                lastSlowWarningNanos = now;
                RevolutionMod.logger.warn("[UI-02-D PERF] slow " + path
                        + " queueMs=" + millis(queueNanos)
                        + " executionMs=" + millis(executionNanos)
                        + " mongoReadMs=" + millis(sample.readNanos)
                        + " mongoWriteMs=" + millis(sample.writeNanos)
                        + " mongoReads=" + sample.reads
                        + " mongoWrites=" + sample.writes
                        + " pending=" + pending);
            }
            if (count < 25) return;
            String message = "[UI-02-D PERF] " + path + " samples=" + count
                    + " avgQueueMs=" + millis(totalQueue / count)
                    + " maxQueueMs=" + millis(maxQueue)
                    + " avgExecutionMs=" + millis(totalExecution / count)
                    + " maxExecutionMs=" + millis(maxExecution)
                    + " avgMongoReadMs=" + millis(totalMongoRead / count)
                    + " avgMongoWriteMs=" + millis(totalMongoWrite / count)
                    + " reads=" + reads + " writes=" + writes
                    + " maxPending=" + maxPending;
            if (millis(maxExecution) >= 50L || millis(maxQueue) >= 50L)
                RevolutionMod.logger.warn(message);
            else RevolutionMod.logger.info(message);
            count = maxPending = reads = writes = 0;
            totalQueue = totalExecution = totalMongoRead = totalMongoWrite = 0L;
            maxQueue = maxExecution = 0L;
        }
    }
}
