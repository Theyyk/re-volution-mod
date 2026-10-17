package com.example.customguimod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared arbitrary-precision value helpers for compact large-number input. */
final class LargeNumberValue {
    private static final int MAX_ILLION_INDEX = 999;
    private static final String[] UNIT_PREFIXES = {"", "U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};
    private static final String[] TENS_SUFFIXES = {"", "Dc", "Vg", "Tg", "Qag", "Qig", "Sxg", "Spg", "Ocg", "Nog"};
    private static final String[] HUNDREDS_SUFFIXES = {"", "Ce", "Du", "Tr", "Qe", "Qn", "Ses", "Sts", "Ot", "Nt"};
    private static final String[] SUFFIXES = buildSuffixes();

    private LargeNumberValue() {}

    static int unitCount() {
        return SUFFIXES.length;
    }

    static String unitSuffix(int index) {
        if (index < 0 || index >= SUFFIXES.length) return "";
        return SUFFIXES[index];
    }

    static BigDecimal parseWithUnit(String rawMantissa, int unitIndex) {
        String raw = rawMantissa == null ? "" : rawMantissa.trim().replace(',', '.');
        if (raw.isEmpty()) throw new NumberFormatException("empty value");
        BigDecimal mantissa = new BigDecimal(raw);
        if (unitIndex < 0 || unitIndex >= SUFFIXES.length) throw new NumberFormatException("invalid unit");
        return normalize(mantissa.movePointRight(unitIndex * 3));
    }

    static BigDecimal parseExact(String raw) {
        String value = raw == null ? "" : raw.trim().replace(',', '.');
        if (value.isEmpty()) throw new NumberFormatException("empty value");
        return normalize(new BigDecimal(value));
    }

    static String canonical(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal normalized = normalize(value);
        return normalized.toPlainString();
    }

    static BigDecimal normalize(BigDecimal value) {
        if (value == null || value.signum() == 0) return BigDecimal.ZERO;
        return value.stripTrailingZeros();
    }

    private static String[] buildSuffixes() {
        List<String> values = new ArrayList<>();
        values.add(""); // x1
        values.add("K"); // thousand

        // unit index = exponent / 3, while illion index = unit index - 1.
        // Keep the familiar K/M/B/T family, then extend the same compact Latin-style
        // construction through the 999th -illion (up to 1E3000).
        for (int illionIndex = 1; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            values.add(illionSuffix(illionIndex));
        }

        return values.toArray(new String[0]);
    }

    private static String illionSuffix(int illionIndex) {
        switch (illionIndex) {
            case 1: return "M";
            case 2: return "B";
            case 3: return "T";
            case 4: return "Qa";
            case 5: return "Qi";
            case 6: return "Sx";
            case 7: return "Sp";
            case 8: return "Oc";
            case 9: return "No";
            default:
                if (illionIndex < 100) return belowHundredSuffix(illionIndex);
                int hundreds = illionIndex / 100;
                int remainder = illionIndex % 100;
                return belowHundredSuffix(remainder) + HUNDREDS_SUFFIXES[hundreds];
        }
    }

    private static String belowHundredSuffix(int value) {
        if (value <= 0) return "";
        int tens = value / 10;
        int ones = value % 10;
        return UNIT_PREFIXES[ones] + TENS_SUFFIXES[tens];
    }
}
