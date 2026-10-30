package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.rune.definition.RuneCollectionId;

/** Server-side Balance Core adapter for selected-rune rank revert policy. */
public final class RuneRankRevertBalanceService {
    private final BalanceService balance;

    public RuneRankRevertBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    public int allowedFromRank(int statusId) {
        return positiveInt(RuneBalanceNamespace.legacyStatus(statusId), "revert_allowed_from_rank");
    }

    public int allowedFromRank(RuneCollectionId collectionId) {
        return positiveInt(RuneBalanceNamespace.collection(collectionId), "revert_allowed_from_rank");
    }

    public int steps(int statusId) {
        return positiveInt(RuneBalanceNamespace.legacyStatus(statusId), "revert_steps");
    }

    public int steps(RuneCollectionId collectionId) {
        return positiveInt(RuneBalanceNamespace.collection(collectionId), "revert_steps");
    }

    private int positiveInt(String namespace, String suffix) {
        String raw = balance.getActiveValue(RuneBalanceNamespace.valueKey(namespace, suffix));
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) throw new NumberFormatException("not positive");
            return value;
        } catch (RuntimeException e) {
            throw new IllegalStateException(suffix + " must be a positive integer: " + raw, e);
        }
    }
}
