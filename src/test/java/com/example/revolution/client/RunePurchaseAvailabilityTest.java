package com.example.revolution.client;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class RunePurchaseAvailabilityTest {
    @Test void exactBalanceIsAvailable() {
        assertTrue(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.TEN, 10));
    }
    @Test void insufficientWalletIsUnavailable() {
        assertFalse(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.valueOf(9), 10));
        assertFalse(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.ZERO, 1));
    }
    @Test void unknownAndLockedStateFailClosed() {
        assertFalse(RunePurchaseAvailability.isAvailable(false, true, false, BigDecimal.TEN, 1));
        assertFalse(RunePurchaseAvailability.isAvailable(true, false, false, BigDecimal.TEN, 1));
        assertFalse(RunePurchaseAvailability.isAvailable(true, true, true, BigDecimal.TEN, 1));
        assertFalse(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.TEN, 0));
    }
    @Test void newerWalletSnapshotChangesEligibility() {
        assertTrue(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.TEN, 10));
        assertFalse(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.ZERO, 10));
        assertTrue(RunePurchaseAvailability.isAvailable(true, true, false, BigDecimal.TEN, 10));
    }
}
