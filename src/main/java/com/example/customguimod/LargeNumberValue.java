package com.example.customguimod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared arbitrary-precision value helpers for compact large-number input. */
final class LargeNumberValue {
    private static final int MAX_UNIT_INDEX = 4095;
    private static final int CENTILLION_INDEX = 101;
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
        String[] base = {
                "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
                "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"
        };
        for (String suffix : base) values.add(suffix);

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

        // Keep all established names through centillion, then continue with a compact,
        // deterministic namespace. Ce is Ce1, the next 1000x step is Ce2, etc.
        // This keeps dropdown labels short while allowing values far beyond 1E1233.
        for (int index = CENTILLION_INDEX + 1; index <= MAX_UNIT_INDEX; index++) {
            values.add("Ce" + (index - CENTILLION_INDEX + 1));
        }

        return values.toArray(new String[0]);
    }

    private static void addDecade(List<String> values, String base) {
        String[] prefixes = {"U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};
        for (String prefix : prefixes) values.add(prefix + base);
    }
}
