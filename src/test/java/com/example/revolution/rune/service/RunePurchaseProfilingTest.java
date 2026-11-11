package com.example.revolution.rune.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RunePurchaseProfilingTest {
    @Test void ordinaryAndCrystalProbesExecuteEachAcceptedRequestExactlyOnce() {
        AtomicInteger ordinary = new AtomicInteger();
        AtomicInteger crystal = new AtomicInteger();
        RunePurchaseProfiling.queued("ordinary");
        RunePurchaseProfiling.process("ordinary", System.nanoTime(), () -> {
            RunePurchaseProfiling.mongoRead(() -> ordinary.incrementAndGet());
            RunePurchaseProfiling.mongoWrite(ordinary::incrementAndGet);
        });
        RunePurchaseProfiling.queued("crystal");
        RunePurchaseProfiling.process("crystal", System.nanoTime(), () -> {
            RunePurchaseProfiling.mongoRead(() -> crystal.incrementAndGet());
            RunePurchaseProfiling.mongoWrite(crystal::incrementAndGet);
        });
        assertEquals(2, ordinary.get());
        assertEquals(2, crystal.get());
    }

    @Test void databaseFailureIsPropagatedAndNextPurchaseStillExecutes() {
        AtomicInteger completed = new AtomicInteger();
        RunePurchaseProfiling.queued("ordinary");
        assertThrows(IllegalStateException.class, () -> RunePurchaseProfiling.process(
                "ordinary", System.nanoTime(),
                () -> RunePurchaseProfiling.mongoWrite(() -> {
                    throw new IllegalStateException("mock write failed");
                })));
        RunePurchaseProfiling.queued("ordinary");
        RunePurchaseProfiling.process("ordinary", System.nanoTime(), completed::incrementAndGet);
        assertEquals(1, completed.get());
    }
}
