package com.example.revolution.admin.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory, session-bound prepare/apply workflow for complete Balance Core snapshots. */
public final class AdminBalanceBatchService {
    private static final long PREVIEW_TTL_MILLIS = 120_000L;
    private static final int MAX_PENDING_PREVIEWS = 16;
    private final BalanceService balance;
    private final Map<String, PreparedPreview> previews = new LinkedHashMap<>();

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
        long now = System.currentTimeMillis();
        previews.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        previews.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
        if (previews.size() >= MAX_PENDING_PREVIEWS) throw new IllegalArgumentException("too many pending balance previews");
        String token = UUID.randomUUID().toString();
        long expiresAt = now + PREVIEW_TTL_MILLIS;
        previews.put(token, new PreparedPreview(owner, expectedActiveRevision, prepared, expiresAt));
        return new Preview(token, expectedActiveRevision, prepared.getRevision(),
                prepared.getConfig().getBalanceSchemaVersion(), expiresAt);
    }

    public synchronized BalanceSnapshot apply(String owner, String token) {
        requireOwner(owner);
        if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("preview token is required");
        PreparedPreview preview = previews.get(token);
        if (preview == null || !preview.owner.equals(owner)) throw new IllegalArgumentException("invalid preview token");
        previews.remove(token); // Apply is one-shot even when the expected base revision has become stale.
        if (preview.expiresAt < System.currentTimeMillis()) throw new IllegalArgumentException("balance preview expired");
        BalanceSnapshot activated = balance.activatePrepared(preview.snapshot, preview.baseRevision);
        previews.clear(); // Any other preview was prepared against the now-replaced active snapshot.
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
        final long expiresAt;
        PreparedPreview(String owner, String baseRevision, BalanceSnapshot snapshot, long expiresAt) {
            this.owner = owner;
            this.baseRevision = baseRevision;
            this.snapshot = snapshot;
            this.expiresAt = expiresAt;
        }
    }

    public static final class Preview {
        public final String token;
        public final String baseRevision;
        public final String candidateRevision;
        public final int balanceSchemaVersion;
        public final long expiresAt;
        Preview(String token, String baseRevision, String candidateRevision, int balanceSchemaVersion, long expiresAt) {
            this.token = token;
            this.baseRevision = baseRevision;
            this.candidateRevision = candidateRevision;
            this.balanceSchemaVersion = balanceSchemaVersion;
            this.expiresAt = expiresAt;
        }
    }
}
