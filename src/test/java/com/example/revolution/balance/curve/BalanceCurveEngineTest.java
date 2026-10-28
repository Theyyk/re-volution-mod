package com.example.revolution.balance.curve;

import com.example.revolution.balance.definition.BalanceCurveDefinition;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Focused pure-math tests for the deterministic curve evaluator. */
public final class BalanceCurveEngineTest {
    @Test
    void evaluatesAllSupportedModes() {
        BalanceCurveEngine engine = new BalanceCurveEngine();

        check("9", engine.evaluate(BalanceCurveDefinition.fixed(new BigDecimal("9")), 100));

        Map<Integer, BigDecimal> table = new LinkedHashMap<>();
        table.put(0, new BigDecimal("2"));
        table.put(1, new BigDecimal("3"));
        check("3", engine.evaluate(BalanceCurveDefinition.table(table), 1));

        check("16", engine.evaluate(BalanceCurveDefinition.formula(new BigDecimal("2"), new BigDecimal("2")), 3));

        BalanceCurveDefinition piecewise = BalanceCurveDefinition.piecewise(Arrays.asList(
                new BalanceCurveDefinition.Piece(0, 1, BalanceCurveDefinition.fixed(new BigDecimal("5"))),
                new BalanceCurveDefinition.Piece(2, null,
                        BalanceCurveDefinition.formula(new BigDecimal("10"), new BigDecimal("3")))));
        check("5", engine.evaluate(piecewise, 1));
        check("10", engine.evaluate(piecewise, 2));
        check("30", engine.evaluate(piecewise, 3));
    }

    private static void check(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("expected " + expected + ", got " + actual.toPlainString());
        }
    }
}
