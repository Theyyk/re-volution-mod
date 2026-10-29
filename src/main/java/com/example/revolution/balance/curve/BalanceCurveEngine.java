package com.example.revolution.balance.curve;

import com.example.revolution.balance.definition.BalanceCurveDefinition;
import java.math.BigDecimal;
import java.util.Map;

/** Pure deterministic evaluator for canonical balance curves. */
public final class BalanceCurveEngine {
    public BigDecimal evaluate(BalanceCurveDefinition curve, int index) {
        if (curve == null) throw new IllegalArgumentException("curve is required");
        if (index < 0) throw new IllegalArgumentException("curve index must be >= 0");
        if (curve.getMode() == null) throw new IllegalArgumentException("curve mode is required");

        switch (curve.getMode()) {
            case FIXED:
                return normalized(required(curve.getFixedValue(), "fixed value"));
            case TABLE:
                BigDecimal tableValue = curve.getTableValues().get(index);
                if (tableValue != null) return normalized(tableValue);
                BigDecimal tailGrowth = curve.getTableTailGrowth();
                if (tailGrowth != null && !curve.getTableValues().isEmpty()) {
                    Map.Entry<Integer, BigDecimal> last = null;
                    for (Map.Entry<Integer, BigDecimal> entry : curve.getTableValues().entrySet()) last = entry;
                    if (last != null && index > last.getKey()) {
                        int exponent = index - last.getKey();
                        return normalized(last.getValue().multiply(tailGrowth.pow(exponent)));
                    }
                }
                throw new IllegalArgumentException("TABLE curve has no value for index " + index);
            case FORMULA:
                BigDecimal base = required(curve.getFormulaBase(), "formula base");
                BigDecimal growth = required(curve.getFormulaGrowth(), "formula growth");
                return normalized(base.multiply(growth.pow(index)));
            case PIECEWISE:
                for (BalanceCurveDefinition.Piece piece : curve.getPieces()) {
                    if (piece != null && piece.contains(index)) return evaluate(piece.getCurve(), index - piece.getFrom());
                }
                throw new IllegalArgumentException("PIECEWISE curve has no segment for index " + index);
            default:
                throw new IllegalArgumentException("unsupported curve mode " + curve.getMode());
        }
    }

    private BigDecimal required(BigDecimal value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    private BigDecimal normalized(BigDecimal value) {
        if (value.signum() == 0) return BigDecimal.ZERO;
        return value.stripTrailingZeros();
    }
}
