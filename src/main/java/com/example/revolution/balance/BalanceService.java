package com.example.revolution.balance;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the active validated balance snapshot.
 * Candidate configs are parsed and validated completely before the active reference is replaced.
 */
public final class BalanceService {
    private final BalanceConfigParser parser;
    private final BalanceValidator validator;
    private final BalanceCurveEngine curveEngine;
    private final AtomicReference<BalanceSnapshot> active = new AtomicReference<>();

    public BalanceService() {
        this(new BalanceConfigParser(), new BalanceValidator(), new BalanceCurveEngine());
    }

    BalanceService(BalanceConfigParser parser, BalanceValidator validator, BalanceCurveEngine curveEngine) {
        this.parser = parser;
        this.validator = validator;
        this.curveEngine = curveEngine;
    }

    public BalanceSnapshot loadInitial(String rawJson) {
        BalanceSnapshot snapshot = prepare(rawJson);
        if (!active.compareAndSet(null, snapshot)) throw new IllegalStateException("initial balance is already loaded");
        return snapshot;
    }

    public BalanceSnapshot apply(String rawJson) {
        BalanceSnapshot candidate = prepare(rawJson);
        active.set(candidate);
        return candidate;
    }

    public BalanceSnapshot getActiveSnapshot() {
        BalanceSnapshot snapshot = active.get();
        if (snapshot == null) throw new IllegalStateException("balance is not loaded");
        return snapshot;
    }

    public String getActiveRevision() {
        return getActiveSnapshot().getRevision();
    }

    public BigDecimal evaluate(String curveId, int index) {
        return curveEngine.evaluate(getActiveSnapshot().getConfig(), curveId, index);
    }

    BalanceSnapshot prepare(String rawJson) {
        BalanceConfig config = parser.parse(rawJson);
        validator.validate(config);
        String canonical = BalanceCanonicalJson.write(config);
        return new BalanceSnapshot(config, sha256(canonical));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
