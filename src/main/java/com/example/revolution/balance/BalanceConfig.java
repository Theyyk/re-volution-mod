package com.example.revolution.balance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable canonical balance document for one schema revision. */
public final class BalanceConfig {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final int balanceSchemaVersion;
    private final Map<String, BalanceCurve> curves;

    public BalanceConfig(int balanceSchemaVersion, Map<String, BalanceCurve> curves) {
        this.balanceSchemaVersion = balanceSchemaVersion;
        this.curves = Collections.unmodifiableMap(new LinkedHashMap<>(curves));
    }

    public int getBalanceSchemaVersion() {
        return balanceSchemaVersion;
    }

    public Map<String, BalanceCurve> getCurves() {
        return curves;
    }

    public BalanceCurve getCurve(String id) {
        return curves.get(id);
    }
}
