package com.example.revolution.balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Structural validator. Domain-specific validation is layered on top by rune/mob modules. */
public final class BalanceValidator {
    public void validate(BalanceConfig config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            errors.add("balance config is null");
            throw new BalanceValidationException(errors);
        }

        if (config.getBalanceSchemaVersion() != BalanceConfig.SUPPORTED_SCHEMA_VERSION) {
            errors.add("unsupported balanceSchemaVersion " + config.getBalanceSchemaVersion()
                    + "; supported=" + BalanceConfig.SUPPORTED_SCHEMA_VERSION);
        }

        for (Map.Entry<String, BalanceCurve> entry : config.getCurves().entrySet()) {
            validateCurve("curves." + entry.getKey(), entry.getValue(), errors);
        }

        if (!errors.isEmpty()) throw new BalanceValidationException(errors);
    }

    private void validateCurve(String path, BalanceCurve curve, List<String> errors) {
        if (curve == null) {
            errors.add(path + " is null");
            return;
        }

        switch (curve.getMode()) {
            case FIXED:
                requireDecimal(path + ".value", curve.getFixedValue(), errors);
                break;
            case TABLE:
                if (curve.getTable().isEmpty()) errors.add(path + ".values must not be empty");
                for (Map.Entry<Integer, BigDecimal> entry : curve.getTable().entrySet()) {
                    if (entry.getKey() < 0) errors.add(path + ".values index must be >= 0: " + entry.getKey());
                    requireDecimal(path + ".values[" + entry.getKey() + "]", entry.getValue(), errors);
                }
                break;
            case FORMULA:
                if (curve.getFormula() == null) {
                    errors.add(path + ".formula is missing");
                } else {
                    requireDecimal(path + ".base", curve.getFormula().getBase(), errors);
                    requireDecimal(path + ".growth", curve.getFormula().getGrowth(), errors);
                    if (curve.getFormula().getGrowth().signum() < 0) errors.add(path + ".growth must be >= 0");
                }
                break;
            case PIECEWISE:
                if (curve.getSegments().isEmpty()) errors.add(path + ".segments must not be empty");
                int previousTo = -1;
                for (int i = 0; i < curve.getSegments().size(); i++) {
                    BalanceCurve.Segment segment = curve.getSegments().get(i);
                    String segmentPath = path + ".segments[" + i + "]";
                    if (segment.getFromInclusive() < 0) errors.add(segmentPath + ".from must be >= 0");
                    if (segment.getToInclusive() < segment.getFromInclusive()) errors.add(segmentPath + ".to must be >= from");
                    if (i > 0 && segment.getFromInclusive() <= previousTo) errors.add(segmentPath + " overlaps a previous segment");
                    if (segment.getCurve() != null && segment.getCurve().getMode() == BalanceCurve.Mode.PIECEWISE) {
                        errors.add(segmentPath + " must not contain nested PIECEWISE curve");
                    } else {
                        validateCurve(segmentPath + ".curve", segment.getCurve(), errors);
                    }
                    previousTo = Math.max(previousTo, segment.getToInclusive());
                }
                break;
            default:
                errors.add(path + " has unsupported mode " + curve.getMode());
        }
    }

    private static void requireDecimal(String path, BigDecimal value, List<String> errors) {
        if (value == null) errors.add(path + " is missing");
    }
}
