package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import org.junit.jupiter.api.Test;

/** Focused application-service behavior for prepare vs activate. */
public final class BalanceServiceTest {
    @Test
    void prepareDoesNotMutateAndFailedActivationKeepsPreviousSnapshot() {
        BalanceService service = new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                new BalanceConfigCodec(),
                new BalanceCurveEngine());

        String first = "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":1}}}";
        String second = "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":2}}}";
        String invalid = "{\"balanceSchemaVersion\":2,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":3}}}";

        BalanceSnapshot active = service.activate(first);
        BalanceSnapshot prepared = service.prepare(second);
        if (!service.getActiveSnapshot().getRevision().equals(active.getRevision())) {
            throw new AssertionError("prepare must not replace active snapshot");
        }
        if (prepared.getRevision().equals(active.getRevision())) {
            throw new AssertionError("different semantic configs must have different revisions");
        }

        try {
            service.activate(invalid);
            throw new AssertionError("invalid activation must fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        if (!service.getActiveSnapshot().getRevision().equals(active.getRevision())) {
            throw new AssertionError("failed activation must preserve previous snapshot");
        }
    }
}
