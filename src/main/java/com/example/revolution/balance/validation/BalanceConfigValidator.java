package com.example.revolution.balance.validation;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.definition.BalanceCurveDefinition;
import com.example.revolution.balance.definition.BalanceCurveMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Semantic validation performed before a balance snapshot can become active. */
public final class BalanceConfigValidator {
    private static final Pattern CURVE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]*");
    private static final int MAX_CURVE_DEPTH = 8;

    public void validate(BalanceConfig config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            errors.add("config is null");
            throw new BalanceValidationException(errors);
        }

        if (config.getBalanceSchemaVersion() != BalanceConfig.SUPPORTED_SCHEMA_VERSION) {
            errors.add("unsupported balanceSchemaVersion " + config.getBalanceSchemaVersion()
                    + ", expected " + BalanceConfig.SUPPORTED_SCHEMA_VERSION);
        }

        if (config.getCurves().isEmpty()) {
            errors.add("curves must contain at least one entry");
        }

        for (Map.Entry<String, BalanceCurveDefinition> entry : config.getCurves().entrySet()) {
            String id = entry.getKey();
            if (id == null || !CURVE_ID.matcher(id).matches()) {
                errors.add("invalid curve id '" + id + "'");
            }
            validateCurve(entry.getValue(), "curves." + id, 0, errors);
        }

        if (!errors.isEmpty()) throw new BalanceValidationException(errors);
    }

    private void validateCurve(BalanceCurveDefinition curve, String path, int depth, List<String> errors) {
        if (curve == null) {
            errors.add(path + " is null");
            return;
        }
        if (depth > MAX_CURVE_DEPTH) {
            errors.add(path + " exceeds maximum nested curve depth " + MAX_CURVE_DEPTH);
            return;
        }
        if (curve.getMode() == null) {
            errors.add(path + ".mode is required");
            return;
        }

        BalanceCurveMode mode = curve.getMode();
        switch (mode) {
            case FIXED:
                validateNonNegative(curve.getFixedValue(), path + ".value", errors);
                break;
            case TABLE:
                validateTable(curve, path, errors);
                break;
            case FORMULA:
                validateNonNegative(curve.getFormulaBase(), path + ".base", errors);
                validateNonNegative(curve.getFormulaGrowth(), path + ".growth", errors);
                break;
            case PIECEWISE:
                validatePieces(curve, path, depth, errors);
                break;
            default:
                errors.add(path + ".mode is unsupported");
        }
    }

    private void validateTable(BalanceCurveDefinition curve, String path, List<String> errors) {
        if (curve.getTableValues().isEmpty()) {
            errors.add(path + ".values must not be empty");
            return;
        }
        int expectedIndex = 0;
        for (Map.Entry<Integer, BigDecimal> entry : curve.getTableValues().entrySet()) {
            if (entry.getKey() == null || entry.getKey() != expectedIndex) {
                errors.add(path + ".values must be contiguous from index 0; expected " + expectedIndex);
                return;
            }
            validateNonNegative(entry.getValue(), path + ".values[" + expectedIndex + "]", errors);
            expectedIndex++;
        }
    }

    private void validatePieces(BalanceCurveDefinition curve, String path, int depth, List<String> errors) {
        if (curve.getPieces().isEmpty()) {
            errors.add(path + ".segments must not be empty");
            return;
        }

        int expectedFrom = 0;
        boolean unboundedSeen = false;
        for (int i = 0; i < curve.getPieces().size(); i++) {
            BalanceCurveDefinition.Piece piece = curve.getPieces().get(i);
            String piecePath = path + ".segments[" + i + "]";
            if (piece == null) {
                errors.add(piecePath + " is null");
                continue;
            }
            if (unboundedSeen) {
                errors.add(piecePath + " appears after an unbounded segment");
            }
            if (piece.getFrom() != expectedFrom) {
                errors.add(piecePath + ".from must be " + expectedFrom + " for contiguous coverage");
            }
            if (piece.getFrom() < 0) {
                errors.add(piecePath + ".from must be >= 0");
            }

            Integer to = piece.getTo();
            if (to == null) {
                unboundedSeen = true;
                if (i != curve.getPieces().size() - 1) {
                    errors.add(piecePath + " may be unbounded only when it is the final segment");
                }
            } else if (to < piece.getFrom()) {
                errors.add(piecePath + ".to must be >= from");
            } else if (to == Integer.MAX_VALUE) {
                errors.add(piecePath + ".to must not be Integer.MAX_VALUE; omit to for an unbounded segment");
            }

            validateCurve(piece.getCurve(), piecePath + ".curve", depth + 1, errors);
            if (to != null && to >= piece.getFrom() && to < Integer.MAX_VALUE) expectedFrom = to + 1;
        }
    }

    private void validateNonNegative(BigDecimal value, String path, List<String> errors) {
        if (value == null) {
            errors.add(path + " is required");
        } else if (value.signum() < 0) {
            errors.add(path + " must be >= 0");
        }
    }
}
