package com.example.customguimod;

import java.math.BigDecimal;
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
        int unitIndex = Math.max(0, (integerDigits - 1) / 3);
        if (mode == LargeNumberDisplayMode.ENGINEERING) {
            return engineering(normalized, unitIndex);
        }

        if (unitIndex < LargeNumberValue.unitCount()) {
            BigDecimal scaled = normalized.movePointLeft(unitIndex * 3);
            BigDecimal rounded = scaled.setScale(2, RoundingMode.HALF_UP);
            if (rounded.abs().compareTo(THOUSAND) >= 0 && unitIndex + 1 < LargeNumberValue.unitCount()) {
                unitIndex++;
                scaled = normalized.movePointLeft(unitIndex * 3);
            }
            return compactPlain(scaled) + LargeNumberValue.unitSuffix(unitIndex, LargeNumberDisplayMode.LETTER);
        }

        return engineering(normalized, unitIndex);
    }

    static String compactPlain(double value) {
        if (!Double.isFinite(value)) return "0";
        return compactPlain(BigDecimal.valueOf(value));
    }

    static String compactPlain(BigDecimal value) {
        if (value == null || value.signum() == 0) return "0";
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String engineering(BigDecimal value, int unitIndex) {
        int exponent = Math.max(0, unitIndex * 3);
        BigDecimal mantissa = value.movePointLeft(exponent);
        BigDecimal rounded = mantissa.setScale(2, RoundingMode.HALF_UP);
        if (rounded.abs().compareTo(THOUSAND) >= 0) {
            exponent += 3;
            mantissa = value.movePointLeft(exponent);
        }
        return compactPlain(mantissa) + "E" + exponent;
    }
}
