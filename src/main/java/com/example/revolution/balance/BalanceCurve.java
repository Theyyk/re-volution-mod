package com.example.revolution.balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Immutable balance curve definition. Domain services consume curves through BalanceService. */
public final class BalanceCurve {
    public enum Mode {
        FIXED,
        TABLE,
        FORMULA,
        PIECEWISE
    }

    public static final class Formula {
        private final BigDecimal base;
        private final BigDecimal growth;

        public Formula(BigDecimal base, BigDecimal growth) {
            this.base = requireValue(base, "base");
            this.growth = requireValue(growth, "growth");
        }

        public BigDecimal getBase() { return base; }
        public BigDecimal getGrowth() { return growth; }
    }

    public static final class Segment {
        private final int fromInclusive;
        private final int toInclusive;
        private final BalanceCurve curve;

        public Segment(int fromInclusive, int toInclusive, BalanceCurve curve) {
            this.fromInclusive = fromInclusive;
            this.toInclusive = toInclusive;
            this.curve = curve;
        }

        public int getFromInclusive() { return fromInclusive; }
        public int getToInclusive() { return toInclusive; }
        public BalanceCurve getCurve() { return curve; }
    }

    private final Mode mode;
    private final BigDecimal fixedValue;
    private final NavigableMap<Integer, BigDecimal> table;
    private final Formula formula;
    private final List<Segment> segments;

    private BalanceCurve(
            Mode mode,
            BigDecimal fixedValue,
            Map<Integer, BigDecimal> table,
            Formula formula,
            List<Segment> segments) {
        this.mode = mode;
        this.fixedValue = fixedValue;
        this.table = Collections.unmodifiableNavigableMap(new TreeMap<>(table));
        this.formula = formula;
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public static BalanceCurve fixed(BigDecimal value) {
        return new BalanceCurve(Mode.FIXED, requireValue(value, "value"), Collections.emptyMap(), null, Collections.emptyList());
    }

    public static BalanceCurve table(Map<Integer, BigDecimal> values) {
        return new BalanceCurve(Mode.TABLE, null, values, null, Collections.emptyList());
    }

    public static BalanceCurve formula(BigDecimal base, BigDecimal growth) {
        return new BalanceCurve(Mode.FORMULA, null, Collections.emptyMap(), new Formula(base, growth), Collections.emptyList());
    }

    public static BalanceCurve piecewise(List<Segment> segments) {
        return new BalanceCurve(Mode.PIECEWISE, null, Collections.emptyMap(), null, segments);
    }

    public Mode getMode() { return mode; }
    public BigDecimal getFixedValue() { return fixedValue; }
    public NavigableMap<Integer, BigDecimal> getTable() { return table; }
    public Formula getFormula() { return formula; }
    public List<Segment> getSegments() { return segments; }

    private static BigDecimal requireValue(BigDecimal value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value.stripTrailingZeros();
    }
}
