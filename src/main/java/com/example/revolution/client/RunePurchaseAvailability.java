package com.example.revolution.client;

import java.math.BigDecimal;

/** UI-only eligibility gate. Purchase authority remains on the server. */
final class RunePurchaseAvailability {
    private RunePurchaseAvailability() {}

    static boolean isAvailable(boolean snapshotReady, boolean unlocked, boolean full,
                               BigDecimal balance, int nextAttemptCost) {
        return snapshotReady && unlocked && !full && nextAttemptCost > 0
                && balance != null
                && balance.compareTo(BigDecimal.valueOf(nextAttemptCost)) >= 0;
    }
}
