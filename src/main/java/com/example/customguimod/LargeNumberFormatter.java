package com.example.customguimod;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Shared presentation-only formatter for large numeric values. */
final class LargeNumberFormatter {
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    private LargeNumberFormatter() {}

    static String format(long value) {
        return format(BigDecimal.valueOf(value));
    }

    static String format(double value) {
        if (!Double.isFinite(value)) return "0";
        return format(BigDecimal.valueOf(value));
    }

    static String format(BigDecimal value) {
        return format(value, LargeNumberDisplaySettings.getMode());
    }

    static String format(BigDecimal value, LargeNumberDisplayMode mode) {
        if (value == null || value.signum() == 0) return "0";

        BigDecimal normalized = LargeNumberValue.normalize(value);
        BigDecimal abs = normalized.abs();
        if (abs.compareTo(THOUSAND) < 0) return compactPlain(normalized);

        int integerDigits = normalized.precision() - normalized.scale();
        int suffixIndex = Math.max(0, (integerDigits - 1) / 3);

        if (suffixIndex > LargeNumberValue.CENTILLION_UNIT_INDEX
                && mode == LargeNumberDisplayMode.SCIENTIFIC) {
            return scientific(normalized);
        }

        if (suffixIndex < LargeNumberValue.unitCount()) {
            BigDecimal scaled = normalized.movePointLeft(suffixIndex * 3);
            BigDecimal rounded = scaled.setScale(2, RoundingMode.HALF_UP);
            if (rounded.abs().compareTo(THOUSAND) >= 0 && suffixIndex + 1 < LargeNumberValue.unitCount()) {
                suffixIndex++;
                scaled = normalized.movePointLeft(suffixIndex * 3);
                if (suffixIndex > LargeNumberValue.CENTILLION_UNIT_INDEX
                        && mode == LargeNumberDisplayMode.SCIENTIFIC) {
                    return scientific(normalized);
                }
            }
            return compactPlain(scaled) + LargeNumberValue.unitSuffix(suffixIndex, mode);
        }

        return scientific(normalized);
    }

    static String compactPlain(double value) {
        if (!Double.isFinite(value)) return "0";
        return compactPlain(BigDecimal.valueOf(value));
    }

    static String compactPlain(BigDecimal value) {
        if (value == null || value.signum() == 0) return "0";
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String scientific(BigDecimal value) {
        BigDecimal rounded = value.round(new MathContext(3, RoundingMode.HALF_UP));
        String text = rounded.toString();
        if (!text.contains("E")) {
            int exponent = value.precision() - value.scale() - 1;
            BigDecimal mantissa = value.movePointLeft(exponent).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
            text = mantissa.toPlainString() + "E" + exponent;
        }
        return text.replace("E+", "E");
    }
}
