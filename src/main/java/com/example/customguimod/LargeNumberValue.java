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
        String[] standard = {
                "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
                "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"
        };
        for (String suffix : standard) values.add(suffix);

        addDecade(values, "Vg");
        values.add("Tg");
        addDecade(values, "Tg");
        values.add("Qag");
        addDecade(values, "Qag");
        values.add("Qig");
        addDecade(values, "Qig");
        values.add("Sxg");
        addDecade(values, "Sxg");
        values.add("Spg");
        addDecade(values, "Spg");
        values.add("Ocg");
        addDecade(values, "Ocg");
        values.add("Nog");
        addDecade(values, "Nog");
        values.add("Ce");

        // Keep the established compact naming unchanged through Ce (1E303).
        // From UCe onward, extend with our Latin-style compositional suffixes.
        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            values.add(extendedIllionSuffix(illionIndex));
        }

        return values.toArray(new String[0]);
    }

    private static String extendedIllionSuffix(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        return belowHundredExtendedSuffix(remainder) + HUNDREDS_SUFFIXES[hundreds];
    }

    private static String belowHundredExtendedSuffix(int value) {
        if (value <= 0) return "";
        int tens = value / 10;
        int ones = value % 10;
        return UNIT_PREFIXES[ones] + TENS_SUFFIXES[tens];
    }

    private static void addDecade(List<String> values, String base) {
        String[] prefixes = {"U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};
        for (String prefix : prefixes) values.add(prefix + base);
    }
}
