package com.example.revolution.admin.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminBalanceBatchServiceTest {
    private static BalanceService balance() {
        return new BalanceService(new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
    }
    private static String config(int value) {
        return "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":" + value + "}}}";
    }

    @Test void previewDoesNotMutateAndApplyActivatesExactPreparedSnapshotOnce() {
        BalanceService balance = balance();
        BalanceSnapshot original = balance.activate(config(1));
        AdminBalanceBatchService service = new AdminBalanceBatchService(balance);
        AdminBalanceBatchService.Preview preview = service.preview("Bearer session-a", original.getRevision(), config(2));
        assertEquals(original.getRevision(), balance.getActiveSnapshot().getRevision());
        assertNotEquals(original.getRevision(), preview.candidateRevision);
        BalanceSnapshot applied = service.apply("Bearer session-a", preview.token);
        assertEquals(preview.candidateRevision, applied.getRevision());
        assertSame(applied, balance.getActiveSnapshot());
        assertThrows(IllegalArgumentException.class, () -> service.apply("Bearer session-a", preview.token));
    }

    @Test void previewIsSessionBoundAndStaleApplyCannotMutateActiveBalance() {
        BalanceService balance = balance();
        BalanceSnapshot original = balance.activate(config(1));
        AdminBalanceBatchService service = new AdminBalanceBatchService(balance);
        AdminBalanceBatchService.Preview preview = service.preview("Bearer session-a", original.getRevision(), config(2));
        assertThrows(IllegalArgumentException.class, () -> service.apply("Bearer session-b", preview.token));
        BalanceSnapshot external = balance.activate(config(3));
        assertThrows(IllegalArgumentException.class, () -> service.apply("Bearer session-a", preview.token));
        assertEquals(external.getRevision(), balance.getActiveSnapshot().getRevision());
    }

    @Test void invalidOrAlreadyStalePreviewLeavesActiveBalanceUntouched() {
        BalanceService balance = balance();
        BalanceSnapshot original = balance.activate(config(1));
        AdminBalanceBatchService service = new AdminBalanceBatchService(balance);
        assertThrows(IllegalArgumentException.class,
                () -> service.preview("Bearer session", "wrong-revision", config(2)));
        assertThrows(IllegalArgumentException.class,
                () -> service.preview("Bearer session", original.getRevision(),
                        "{\"balanceSchemaVersion\":2,\"curves\":{}}"));
        assertEquals(original.getRevision(), balance.getActiveSnapshot().getRevision());
        assertEquals(0, service.pendingPreviewCount());
    }
}
