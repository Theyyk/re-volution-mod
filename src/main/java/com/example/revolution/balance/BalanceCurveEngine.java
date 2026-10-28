package com.example.revolution.balance;

import java.math.BigDecimal;
import java.util.Map;

/** Pure evaluator for canonical balance curves. */
public final class BalanceCurveEngine {
    public BigDecimal evaluate(BalanceCurve curve, int index) {
        if (curve == null) throw new IllegalArgumentException("curve must not be null");
        if (index < 0) throw new IllegalArgumentException("curve index must be >= 0");

        switch (curve.getMode()) {
            case FIXED:
                return normalize(curve.getFixedValue());
            case TABLE:
                BigDecimal tableValue = curve.getTable().get(index);
                if (tableValue == null) throw new IllegalArgumentException("TABLE curve has no value for index " + index);
                return normalize(tableValue);
            case FORMULA:
                BalanceCurve.Formula formula = curve.getFormula();
                return normalize(formula.getBase().multiply(formula.getGrowth().pow(index)));
            case PIECEWISE:
                for (BalanceCurve.Segment segment : curve.getSegments()) {
                    if (index < segment.getFromInclusive() || index > segment.getToInclusive()) continue;
                    int localIndex = index - segment.getFromInclusive();
                    return evaluate(segment.getCurve(), localIndex);
                }
                throw new IllegalArgumentException("PIECEWISE curve has no segment for index " + index);
            default:
                throw new IllegalArgumentException("unsupported curve mode " + curve.getMode());
        }
    }

    public BigDecimal evaluate(BalanceConfig config, String curveId, int index) {
        if (config == null) throw new IllegalArgumentException("balance config must not be null");
        BalanceCurve curve = config.getCurve(curveId);
        if (curve == null) throw new IllegalArgumentException("unknown balance curve '" + curveId + "'");
        return evaluate(curve, index);
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null || value.signum() == 0) return BigDecimal.ZERO;
        return value.stripTrailingZeros();
    }
}
