package com.example.revolution.client;

import java.math.BigDecimal;

/** UI-only eligibility gate. Purchase authority remains on the server. */
final class RunePurchaseAvailability {
    private RunePurchaseAvailability() {}

    /** Retain the last price across the purchase round-trip; pending state is not a visual state. */
    static String purchaseLabel(boolean snapshotReady, int nextAttemptCost, int amount,
                                String resourceName, String lastVisibleLabel) {
        if (!snapshotReady) return lastVisibleLabel == null || lastVisibleLabel.isEmpty()
                ? "Купить руну" : lastVisibleLabel;
        if (nextAttemptCost <= 0) return "Купить руну:\nНедоступно";
        return "Купить руну:\n" + (amount > 1 ? "от " : "")
                + nextAttemptCost + " " + resourceName;
    }

    static boolean isAvailable(boolean snapshotReady, boolean unlocked, boolean full,
                               BigDecimal balance, int nextAttemptCost) {
        return snapshotReady && unlocked && !full && nextAttemptCost > 0
                && balance != null
                && balance.compareTo(BigDecimal.valueOf(nextAttemptCost)) >= 0;
    }
}
