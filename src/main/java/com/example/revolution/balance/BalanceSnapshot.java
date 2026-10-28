package com.example.revolution.balance;

/** Immutable active balance state with semantic revision hash. */
public final class BalanceSnapshot {
    private final BalanceConfig config;
    private final String revision;

    public BalanceSnapshot(BalanceConfig config, String revision) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        if (revision == null || revision.trim().isEmpty()) throw new IllegalArgumentException("revision must not be blank");
        this.config = config;
        this.revision = revision;
    }

    public BalanceConfig getConfig() { return config; }
    public String getRevision() { return revision; }
}
