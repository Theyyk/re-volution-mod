package com.example.customguimod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared arbitrary-precision value helpers for compact large-number input. */
final class LargeNumberValue {
    static final int CENTILLION_UNIT_INDEX = 101;
    private static final int MAX_ILLION_INDEX = 999;
    private static final String[] UNIT_PREFIXES = {"", "U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};
    private static final String[] TENS_SUFFIXES = {"", "Dc", "Vg", "Tg", "Qag", "Qig", "Sxg", "Spg", "Ocg", "Nog"};
    private static final String[] HUNDREDS_SUFFIXES = {"", "Ce", "Du", "Tr", "Qe", "Qn", "Ses", "Sts", "Ot", "Nt"};
    private static final String[] SUFFIXES = buildSuffixes();

    private LargeNumberValue() {}

    static int unitCount() { return SUFFIXES.length; }

    static String unitSuffix(int index) { return unitSuffix(index, LargeNumberDisplayMode.FULL); }

    static String unitSuffix(int index, LargeNumberDisplayMode mode) {
        if (index < 0 || index >= SUFFIXES.length) return "";
        if (index <= CENTILLION_UNIT_INDEX || mode == null || mode == LargeNumberDisplayMode.FULL) return SUFFIXES[index];
        if (mode == LargeNumberDisplayMode.SCIENTIFIC) return "";
        int illionIndex = index - 1;
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        int tens = remainder / 10;
        int ones = remainder % 10;
        StringBuilder result = new StringBuilder();
        if (ones > 0) result.append(componentInitial(UNIT_PREFIXES[ones]));
        if (tens > 0) result.append(componentInitial(TENS_SUFFIXES[tens]));
        if (hundreds > 0) {
            String hundredsSuffix = HUNDREDS_SUFFIXES[hundreds];
            result.append(mode == LargeNumberDisplayMode.ABBREVIATED ? hundredsSuffix : componentInitial(hundredsSuffix));
        }
        return result.toString();
    }

    static BigDecimal parseWithUnit(String rawMantissa, int unitIndex) {
        String raw = rawMantissa == null ? "" : rawMantissa.trim().replace(',', '.');
        if (raw.isEmpty()) throw new NumberFormatException("empty value");
        BigDecimal mantissa = new BigDecimal(raw);
        if (unitIndex < 0 || unitIndex >= SUFFIXES.length) throw new NumberFormatException("invalid unit");
        return normalize(mantissa.movePointRight(unitIndex * 3));
    }

    /** Accepts plain/scientific numbers and full compact suffixes such as 1T, 1Ce or 1QaQagQe. */
    static BigDecimal parseFlexible(String raw) {
        String value = raw == null ? "" : raw.trim().replace(',', '.');
        if (value.isEmpty()) throw new NumberFormatException("empty value");
        try {
            return normalize(new BigDecimal(value));
        } catch (NumberFormatException ignored) {
            for (int i = SUFFIXES.length - 1; i >= 1; i--) {
                String suffix = SUFFIXES[i];
                if (!value.endsWith(suffix) || value.length() <= suffix.length()) continue;
                String mantissa = value.substring(0, value.length() - suffix.length()).trim();
                return parseWithUnit(mantissa, i);
            }
            throw new NumberFormatException("invalid large number: " + raw);
        }
    }

    static BigDecimal parseExact(String raw) {
        String value = raw == null ? "" : raw.trim().replace(',', '.');
        if (value.isEmpty()) throw new NumberFormatException("empty value");
        return normalize(new BigDecimal(value));
    }

    static String canonical(BigDecimal value) {
        if (value == null) return "0";
        return normalize(value).toPlainString();
    }

    static BigDecimal normalize(BigDecimal value) {
        if (value == null || value.signum() == 0) return BigDecimal.ZERO;
        return value.stripTrailingZeros();
    }

    private static String[] buildSuffixes() {
        List<String> values = new ArrayList<>();
        String[] standard = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
                "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"};
        for (String suffix : standard) values.add(suffix);
        addDecade(values, "Vg"); values.add("Tg"); addDecade(values, "Tg");
        values.add("Qag"); addDecade(values, "Qag"); values.add("Qig"); addDecade(values, "Qig");
        values.add("Sxg"); addDecade(values, "Sxg"); values.add("Spg"); addDecade(values, "Spg");
        values.add("Ocg"); addDecade(values, "Ocg"); values.add("Nog"); addDecade(values, "Nog"); values.add("Ce");
        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) values.add(extendedIllionSuffix(illionIndex));
        return values.toArray(new String[0]);
    }

    private static String extendedIllionSuffix(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        return belowHundredExtendedSuffix(remainder) + HUNDREDS_SUFFIXES[hundreds];
    }

    private static String belowHundredExtendedSuffix(int value) {
        if (value <= 0) return "";
        int tens = value / 10, ones = value % 10;
        return UNIT_PREFIXES[ones] + TENS_SUFFIXES[tens];
    }

    private static String componentInitial(String component) { return component == null || component.isEmpty() ? "" : component.substring(0, 1); }

    private static void addDecade(List<String> values, String base) {
        String[] prefixes = {"U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};
        for (String prefix : prefixes) values.add(prefix + base);
    }
}
