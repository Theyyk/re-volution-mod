package com.example.revolution.client;

import java.math.BigDecimal;

/** UI-only eligibility gate. Purchase authority remains on the server. */
final class RunePurchaseAvailability {
    private RunePurchaseAvailability() {}

    /** Keep pending synchronization invisible to players; the separate eligibility gate stays closed. */
    static String purchaseLabel(boolean snapshotReady, int nextAttemptCost, int amount, String resourceName) {
        if (!snapshotReady) return "Купить руну";
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
