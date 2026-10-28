package com.example.revolution.balance.definition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable data-only definition of one balance curve. */
public final class BalanceCurveDefinition {
    private final BalanceCurveMode mode;
    private final BigDecimal fixedValue;
    private final Map<Integer, BigDecimal> tableValues;
    private final BigDecimal formulaBase;
    private final BigDecimal formulaGrowth;
    private final List<Piece> pieces;

    private BalanceCurveDefinition(
            BalanceCurveMode mode,
            BigDecimal fixedValue,
            Map<Integer, BigDecimal> tableValues,
            BigDecimal formulaBase,
            BigDecimal formulaGrowth,
            List<Piece> pieces) {
        this.mode = mode;
        this.fixedValue = fixedValue;
        this.tableValues = tableValues == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new TreeMap<>(tableValues));
        this.formulaBase = formulaBase;
        this.formulaGrowth = formulaGrowth;
        this.pieces = pieces == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(pieces));
    }

    public static BalanceCurveDefinition fixed(BigDecimal value) {
        return new BalanceCurveDefinition(BalanceCurveMode.FIXED, value, null, null, null, null);
    }

    public static BalanceCurveDefinition table(Map<Integer, BigDecimal> values) {
        return new BalanceCurveDefinition(BalanceCurveMode.TABLE, null, values, null, null, null);
    }

    public static BalanceCurveDefinition formula(BigDecimal base, BigDecimal growth) {
        return new BalanceCurveDefinition(BalanceCurveMode.FORMULA, null, null, base, growth, null);
    }

    public static BalanceCurveDefinition piecewise(List<Piece> pieces) {
        return new BalanceCurveDefinition(BalanceCurveMode.PIECEWISE, null, null, null, null, pieces);
    }

    public BalanceCurveMode getMode() {
        return mode;
    }

    public BigDecimal getFixedValue() {
        return fixedValue;
    }

    public Map<Integer, BigDecimal> getTableValues() {
        return tableValues;
    }

    public BigDecimal getFormulaBase() {
        return formulaBase;
    }

    public BigDecimal getFormulaGrowth() {
        return formulaGrowth;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    /** Inclusive range. A null {@code to} means the final segment is unbounded. */
    public static final class Piece {
        private final int from;
        private final Integer to;
        private final BalanceCurveDefinition curve;

        public Piece(int from, Integer to, BalanceCurveDefinition curve) {
            this.from = from;
            this.to = to;
            this.curve = curve;
        }

        public int getFrom() {
            return from;
        }

        public Integer getTo() {
            return to;
        }

        public BalanceCurveDefinition getCurve() {
            return curve;
        }

        public boolean contains(int index) {
            return index >= from && (to == null || index <= to);
        }
    }
}
