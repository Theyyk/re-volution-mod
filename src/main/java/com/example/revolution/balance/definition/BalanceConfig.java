package com.example.revolution.balance.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable canonical balance snapshot before runtime activation. */
public final class BalanceConfig {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final int balanceSchemaVersion;
    private final Map<String, String> values;
    private final Map<String, BalanceCurveDefinition> curves;

    public BalanceConfig(int balanceSchemaVersion, Map<String, BalanceCurveDefinition> curves) {
        this(balanceSchemaVersion, Collections.emptyMap(), curves);
    }

    public BalanceConfig(
            int balanceSchemaVersion,
            Map<String, String> values,
            Map<String, BalanceCurveDefinition> curves) {
        this.balanceSchemaVersion = balanceSchemaVersion;
        this.values = values == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.curves = curves == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(curves));
    }

    public int getBalanceSchemaVersion() {
        return balanceSchemaVersion;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public String getValue(String id) {
        return values.get(id);
    }

    public Map<String, BalanceCurveDefinition> getCurves() {
        return curves;
    }

    public BalanceCurveDefinition getCurve(String id) {
        return curves.get(id);
    }
}
