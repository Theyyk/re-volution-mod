package com.example.revolution.balance.service;

import com.example.revolution.balance.definition.BalanceConfig;

/** Immutable validated balance config together with its deterministic revision. */
public final class BalanceSnapshot {
    private final BalanceConfig config;
    private final String revision;

    public BalanceSnapshot(BalanceConfig config, String revision) {
        if (config == null) throw new IllegalArgumentException("config is required");
        if (revision == null || revision.trim().isEmpty()) throw new IllegalArgumentException("revision is required");
        this.config = config;
        this.revision = revision;
    }

    public BalanceConfig getConfig() {
        return config;
    }

    public String getRevision() {
        return revision;
    }
}
