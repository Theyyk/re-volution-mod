package com.example.customguimod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Shared presentation-only formatter for large numeric values.
 * Gameplay/storage values stay untouched; only the text shown to the player is compacted.
 */
final class LargeNumberFormatter {
    private static final double THOUSAND = 1000.0D;
    private static final String[] SUFFIXES = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"
    };

    private LargeNumberFormatter() {}

    static String format(long value) {
        return format((double) value);
    }

    static String format(double value) {
        if (!Double.isFinite(value)) return "0";
        if (value == 0.0D) return "0";

        double abs = Math.abs(value);
        if (abs < THOUSAND) return compactPlain(value);

        int suffixIndex = 0;
        double scaled = value;
        while (Math.abs(scaled) >= THOUSAND && suffixIndex < SUFFIXES.length - 1) {
            scaled /= THOUSAND;
            suffixIndex++;
        }

        scaled = roundToTwoDecimals(scaled);
        if (Math.abs(scaled) >= THOUSAND && suffixIndex < SUFFIXES.length - 1) {
            scaled /= THOUSAND;
            suffixIndex++;
            scaled = roundToTwoDecimals(scaled);
        }

        if (suffixIndex == SUFFIXES.length - 1 && Math.abs(scaled) >= THOUSAND) {
            return scientific(value);
        }

        return compactPlain(scaled) + SUFFIXES[suffixIndex];
    }

    static String compactPlain(double value) {
        if (!Double.isFinite(value)) return "0";
        if (value == 0.0D) return "0";
        if (value == Math.rint(value) && value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }

        BigDecimal decimal = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return decimal.toPlainString();
    }

    private static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static String scientific(double value) {
        String text = String.format(Locale.ROOT, "%.2E", value);
        text = text.replaceAll("\\.00E", "E");
        text = text.replaceAll("(\\.[0-9])0E", "$1E");
        text = text.replace("E+", "E");
        return text;
    }
}
