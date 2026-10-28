package com.example.revolution.admin.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory, session-bound prepare/apply workflow for complete Balance Core snapshots. */
public final class AdminBalanceBatchService {
    private final BalanceService balance;
    private final Map<String, PreparedPreview> previews = new HashMap<>();

    public AdminBalanceBatchService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    public synchronized Preview preview(String owner, String expectedActiveRevision, String rawJson) {
        requireOwner(owner);
        if (expectedActiveRevision == null || expectedActiveRevision.trim().isEmpty()) {
            throw new IllegalArgumentException("active revision is required");
        }
        BalanceSnapshot active = balance.getActiveSnapshot();
        if (active == null || !expectedActiveRevision.equals(active.getRevision())) {
            throw new IllegalArgumentException("active balance changed before preview");
        }
        BalanceSnapshot prepared = balance.prepare(rawJson);
        String token = UUID.randomUUID().toString();
        previews.put(token, new PreparedPreview(owner, expectedActiveRevision, prepared));
        return new Preview(token, expectedActiveRevision, prepared.getRevision(),
                prepared.getConfig().getBalanceSchemaVersion());
    }

    public synchronized BalanceSnapshot apply(String owner, String token) {
        requireOwner(owner);
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("preview token is required");
        PreparedPreview preview = previews.get(token);
        if (preview == null || !preview.owner.equals(owner)) throw new IllegalArgumentException("invalid preview token");
        BalanceSnapshot activated = balance.activatePrepared(preview.snapshot, preview.baseRevision);
        previews.remove(token);
        return activated;
    }

    public synchronized int pendingPreviewCount() {
        return previews.size();
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.trim().isEmpty()) throw new IllegalArgumentException("preview owner is required");
    }

    private static final class PreparedPreview {
        final String owner;
        final String baseRevision;
        final BalanceSnapshot snapshot;
        PreparedPreview(String owner, String baseRevision, BalanceSnapshot snapshot) {
            this.owner = owner;
            this.baseRevision = baseRevision;
            this.snapshot = snapshot;
        }
    }

    public static final class Preview {
        public final String token;
        public final String baseRevision;
        public final String candidateRevision;
        public final int balanceSchemaVersion;
        Preview(String token, String baseRevision, String candidateRevision, int balanceSchemaVersion) {
            this.token = token;
            this.baseRevision = baseRevision;
            this.candidateRevision = candidateRevision;
            this.balanceSchemaVersion = balanceSchemaVersion;
        }
    }
}
