package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.definition.BalanceCurveDefinition;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/** Application service for prepare/validate/activate of complete balance snapshots. */
public final class BalanceService {
    private final BalanceConfigParser parser;
    private final BalanceConfigValidator validator;
    private final BalanceConfigCodec codec;
    private final BalanceCurveEngine curveEngine;
    private final AtomicReference<BalanceSnapshot> active = new AtomicReference<>();

    public BalanceService(
            BalanceConfigParser parser,
            BalanceConfigValidator validator,
            BalanceConfigCodec codec,
            BalanceCurveEngine curveEngine) {
        if (parser == null || validator == null || codec == null || curveEngine == null) {
            throw new IllegalArgumentException("balance service dependencies are required");
        }
        this.parser = parser;
        this.validator = validator;
        this.codec = codec;
        this.curveEngine = curveEngine;
    }

    /** Parse and validate without mutating active state. */
    public BalanceSnapshot prepare(String rawJson) {
        BalanceConfig config = parser.parse(rawJson);
        validator.validate(config);
        return new BalanceSnapshot(config, codec.revision(config));
    }

    /** Atomic all-or-nothing activation: active state changes only after prepare succeeds. */
    public BalanceSnapshot activate(String rawJson) {
        BalanceSnapshot prepared = prepare(rawJson);
        active.set(prepared);
        return prepared;
    }

    public BalanceSnapshot getActiveSnapshot() {
        return active.get();
    }

    public BigDecimal evaluateActiveCurve(String curveId, int index) {
        BalanceSnapshot snapshot = active.get();
        if (snapshot == null) throw new IllegalStateException("no active balance config");
        BalanceCurveDefinition curve = snapshot.getConfig().getCurve(curveId);
        if (curve == null) throw new IllegalArgumentException("unknown balance curve '" + curveId + "'");
        return curveEngine.evaluate(curve, index);
    }
}
