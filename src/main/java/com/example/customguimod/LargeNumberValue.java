package com.example.customguimod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared arbitrary-precision value helpers for compact large-number input. */
final class LargeNumberValue {
    static final int CENTILLION_UNIT_INDEX = 101;
    private static final int MAX_ILLION_INDEX = 999;

    private static final String[] SMALL_SUFFIXES = {
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No",
            "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod"
    };

    private static final String[] VIGINTILLION_BLOCK = {
            "Vg", "Uvg", "Dvg", "Tvg", "Qavg", "Qivg", "Sxvg", "Spvg", "Ocvg", "Nov"
    };

    private static final String[] TRIGINTILLION_BLOCK = {
            "Tt", "Unt", "Dtt", "Ttt", "Qut", "Qat", "Sst", "Spt", "Oct", "Nvt"
    };

    private static final String[] DECADE_ROOTS = {
            "", "Dc", "Vg", "Tt", "Qdt", "Qqt", "Sxt", "Sgt", "Oi", "Noi"
    };

    private static final String[] GENERIC_PREFIXES = {
            "", "U", "D", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "Nv"
    };

    private static final String[] HUNDREDS_ROOTS = {
            "", "C", "Cnn", "Trcn", "Qdcn", "Qncn", "Sxcn", "Spcn", "Occn", "Nvcn"
    };

    private static final String[] SUFFIXES = buildSuffixes();

    private LargeNumberValue() {}

    static int unitCount() { return SUFFIXES.length; }

    static String unitSuffix(int index) { return unitSuffix(index, LargeNumberDisplaySettings.getMode()); }

    static String unitSuffix(int index, LargeNumberDisplayMode mode) {
        if (index < 0 || index >= SUFFIXES.length) return "";
        if (index == 0) return "";
        if (mode == LargeNumberDisplayMode.ENGINEERING) return "E" + (index * 3);
        return SUFFIXES[index];
    }

    static BigDecimal parseWithUnit(String rawMantissa, int unitIndex) {
        String raw = rawMantissa == null ? "" : rawMantissa.trim().replace(',', '.');
        if (raw.isEmpty()) throw new NumberFormatException("empty value");
        BigDecimal mantissa = new BigDecimal(raw);
        if (unitIndex < 0 || unitIndex >= SUFFIXES.length) throw new NumberFormatException("invalid unit");
        return normalize(mantissa.movePointRight(unitIndex * 3));
    }

    /** Accepts plain/scientific numbers and the canonical letter suffixes. */
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
        for (String suffix : SMALL_SUFFIXES) values.add(suffix);
        for (String suffix : VIGINTILLION_BLOCK) values.add(suffix);
        for (String suffix : TRIGINTILLION_BLOCK) values.add(suffix);

        for (int illionIndex = 40; illionIndex < 100; illionIndex++) {
            values.add(letterBelowHundred(illionIndex));
        }
        values.add("C");

        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            values.add(letterExtended(illionIndex));
        }
        return values.toArray(new String[0]);
    }

    private static String letterBelowHundred(int illionIndex) {
        int tens = illionIndex / 10;
        int ones = illionIndex % 10;
        String root = DECADE_ROOTS[tens];
        if (ones == 0) return root;
        return GENERIC_PREFIXES[ones] + root.toLowerCase();
    }

    private static String letterExtended(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        String hundredRoot = HUNDREDS_ROOTS[Math.min(hundreds, HUNDREDS_ROOTS.length - 1)];
        if (remainder == 0) return hundredRoot;
        if (hundreds == 1 && remainder >= 20 && remainder < 30) {
            int ones = remainder % 10;
            if (ones == 0) return "Vgn";
            String[] block = {"Vgn", "Uvgn", "Dvgn", "Tvgn", "Qavgn", "Qivgn", "Sxvgn", "Spvgn", "Ocvgn", "Nvgn"};
            return block[ones];
        }
        String lower = remainder < 100 ? letterBelowHundred(remainder) : "";
        return lower + hundredRoot;
    }
}
