package com.example.customguimod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private static final String[] LEGACY_HUNDREDS_ROOTS = {
            "", "C", "Cnn", "Trcn", "Qdcn", "Qncn", "Sxcn", "Spcn", "Occn", "Nvcn"
    };

    private static final String[] SIMPLIFIED_HUNDRED_ROOTS = {
            "", "Ce", "Duc", "Trc", "Qac", "Qic", "Sxc", "Spc", "Occ", "Ngc"
    };

    private static final String[] SIMPLIFIED_HUNDRED_CODES = {
            "", "c", "du", "tr", "qa", "qi", "sx", "sp", "oc", "ng"
    };

    private static final String[] SIMPLIFIED_ONES = {
            "", "u", "d", "t", "qa", "qi", "sx", "sp", "oc", "nv"
    };

    private static final String[] SIMPLIFIED_TENS = {
            "", "dc", "vg", "tt", "qd", "qq", "sxt", "sgt", "oi", "nn"
    };

    private static final String[] FULL_HUNDRED_ROOTS = {
            "", "Ce", "Duce", "Trece", "Quace", "Quing", "Sesce", "Septing", "Octing", "Nong"
    };

    private static final String[] FULL_HUNDRED_STEMS = {
            "", "ce", "duce", "trece", "quace", "quing", "sesce", "sept", "oct", "nong"
    };

    private static final String[] FULL_ONES = {
            "", "un", "duo", "tre", "qua", "qui", "sex", "sep", "oct", "nov"
    };

    private static final String[] FULL_TENS = {
            "", "de", "vig", "tri", "qua", "qui", "sex", "sep", "oct", "non"
    };

    private static final String[] SIMPLIFIED_SUFFIXES = buildSimplifiedSuffixes();
    private static final String[] FULL_SUFFIXES = buildFullSuffixes(SIMPLIFIED_SUFFIXES);
    private static final String[] LEGACY_SUFFIXES = buildLegacySuffixes();

    private LargeNumberValue() {}

    static int unitCount() { return SIMPLIFIED_SUFFIXES.length; }

    static String unitSuffix(int index) { return unitSuffix(index, LargeNumberDisplaySettings.getMode()); }

    static String unitSuffix(int index, LargeNumberDisplayMode mode) {
        if (index < 0 || index >= unitCount()) return "";
        if (index == 0) return "";
        LargeNumberDisplayMode effectiveMode = mode == null ? LargeNumberDisplayMode.SIMPLIFIED : mode;
        if (effectiveMode == LargeNumberDisplayMode.ENGINEERING) return "E" + (index * 3);
        return effectiveMode == LargeNumberDisplayMode.FULL ? FULL_SUFFIXES[index] : SIMPLIFIED_SUFFIXES[index];
    }

    static BigDecimal parseWithUnit(String rawMantissa, int unitIndex) {
        String raw = rawMantissa == null ? "" : rawMantissa.trim().replace(',', '.');
        if (raw.isEmpty()) throw new NumberFormatException("empty value");
        BigDecimal mantissa = new BigDecimal(raw);
        if (unitIndex < 0 || unitIndex >= unitCount()) throw new NumberFormatException("invalid unit");
        return normalize(mantissa.movePointRight(unitIndex * 3));
    }

    /** Accepts plain/scientific numbers, both current letter styles, and legacy letter suffixes. */
    static BigDecimal parseFlexible(String raw) {
        String value = raw == null ? "" : raw.trim().replace(',', '.');
        if (value.isEmpty()) throw new NumberFormatException("empty value");
        try {
            return normalize(new BigDecimal(value));
        } catch (NumberFormatException ignored) {
            BigDecimal parsed = trySuffixTable(value, suffixesFor(LargeNumberDisplaySettings.getMode()));
            if (parsed != null) return parsed;
            parsed = trySuffixTable(value, SIMPLIFIED_SUFFIXES);
            if (parsed != null) return parsed;
            parsed = trySuffixTable(value, FULL_SUFFIXES);
            if (parsed != null) return parsed;
            parsed = trySuffixTable(value, LEGACY_SUFFIXES);
            if (parsed != null) return parsed;
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

    private static String[] suffixesFor(LargeNumberDisplayMode mode) {
        return mode == LargeNumberDisplayMode.FULL ? FULL_SUFFIXES : SIMPLIFIED_SUFFIXES;
    }

    private static BigDecimal trySuffixTable(String value, String[] suffixes) {
        if (suffixes == null) return null;
        for (int i = suffixes.length - 1; i >= 1; i--) {
            String suffix = suffixes[i];
            if (suffix.isEmpty() || !value.endsWith(suffix) || value.length() <= suffix.length()) continue;
            String mantissa = value.substring(0, value.length() - suffix.length()).trim();
            try {
                return parseWithUnit(mantissa, i);
            } catch (NumberFormatException ignored) {
                // Keep scanning: a shorter legacy suffix can be a tail of a newer suffix.
            }
        }
        return null;
    }

    private static List<String> buildBaseSuffixes(String centillionSuffix) {
        List<String> values = new ArrayList<>();
        for (String suffix : SMALL_SUFFIXES) values.add(suffix);
        for (String suffix : VIGINTILLION_BLOCK) values.add(suffix);
        for (String suffix : TRIGINTILLION_BLOCK) values.add(suffix);
        for (int illionIndex = 40; illionIndex < 100; illionIndex++) values.add(letterBelowHundred(illionIndex));
        values.add(centillionSuffix);
        return values;
    }

    private static String[] buildSimplifiedSuffixes() {
        List<String> values = buildBaseSuffixes("Ce");
        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            values.add(simplifiedExtended(illionIndex));
        }
        return values.toArray(new String[0]);
    }

    private static String[] buildFullSuffixes(String[] simplified) {
        List<String> values = buildBaseSuffixes("Ce");
        Map<String, Integer> used = new HashMap<>();
        for (int i = 1; i < simplified.length; i++) used.put(simplified[i], i);

        Map<String, Integer> reserved = new HashMap<>();
        for (int hundreds = 1; hundreds <= 9; hundreds++) {
            reserved.put(FULL_HUNDRED_ROOTS[hundreds], hundreds * 100 + 1);
        }
        reserved.put("Vigce", 121);
        reserved.put("Unvigce", 122);
        reserved.put("Novnong", 1000);

        for (int i = 1; i < values.size(); i++) used.put(values.get(i), i);
        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            int unitIndex = illionIndex + 1;
            String candidate = fullExtended(illionIndex);
            String unique = uniqueSuffix(candidate, unitIndex, used, reserved);
            values.add(unique);
            used.put(unique, unitIndex);
        }
        return values.toArray(new String[0]);
    }

    private static String[] buildLegacySuffixes() {
        List<String> values = buildBaseSuffixes("C");
        for (int illionIndex = 101; illionIndex <= MAX_ILLION_INDEX; illionIndex++) {
            values.add(legacyExtended(illionIndex));
        }
        return values.toArray(new String[0]);
    }

    private static String letterBelowHundred(int illionIndex) {
        int tens = illionIndex / 10;
        int ones = illionIndex % 10;
        String root = DECADE_ROOTS[tens];
        if (ones == 0) return root;
        return titleCaseSuffix(GENERIC_PREFIXES[ones] + root.toLowerCase(Locale.ROOT));
    }

    private static String simplifiedExtended(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        if (remainder == 0) return SIMPLIFIED_HUNDRED_ROOTS[hundreds];
        if (illionIndex == 199) return "Nvnnce";
        if (illionIndex == 999) return "Nvnnc";

        String remainderCode = simplifiedRemainder(remainder);
        if (hundreds == 1) {
            if (remainder < 10) return titleCaseSuffix(remainderCode + "ce");
            return titleCaseSuffix(remainderCode + "c");
        }
        return titleCaseSuffix(remainderCode + "i" + SIMPLIFIED_HUNDRED_CODES[hundreds]);
    }

    private static String simplifiedRemainder(int remainder) {
        int tens = remainder / 10;
        int ones = remainder % 10;
        return SIMPLIFIED_ONES[ones] + SIMPLIFIED_TENS[tens];
    }

    private static String fullExtended(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        if (remainder == 0) return FULL_HUNDRED_ROOTS[hundreds];
        if (illionIndex == 999) return "Novnong";
        return titleCaseSuffix(fullRemainder(remainder) + FULL_HUNDRED_STEMS[hundreds]);
    }

    private static String fullRemainder(int remainder) {
        int tens = remainder / 10;
        int ones = remainder % 10;
        return FULL_ONES[ones] + FULL_TENS[tens];
    }

    private static String uniqueSuffix(String candidate, int unitIndex, Map<String, Integer> used, Map<String, Integer> reserved) {
        String normalized = titleCaseSuffix(candidate);
        int attempt = 0;
        while (isTakenByOtherIndex(normalized, unitIndex, used, reserved)) {
            normalized = titleCaseSuffix(candidate + alphabeticCounter(attempt++));
        }
        return normalized;
    }

    private static boolean isTakenByOtherIndex(String suffix, int unitIndex, Map<String, Integer> used, Map<String, Integer> reserved) {
        Integer reservedIndex = reserved.get(suffix);
        if (reservedIndex != null && reservedIndex != unitIndex) return true;
        Integer usedIndex = used.get(suffix);
        return usedIndex != null && usedIndex != unitIndex;
    }

    private static String alphabeticCounter(int value) {
        StringBuilder result = new StringBuilder();
        int current = Math.max(0, value);
        do {
            result.append((char) ('a' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
        return result.reverse().toString();
    }

    private static String titleCaseSuffix(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String legacyExtended(int illionIndex) {
        int hundreds = illionIndex / 100;
        int remainder = illionIndex % 100;
        String hundredRoot = LEGACY_HUNDREDS_ROOTS[Math.min(hundreds, LEGACY_HUNDREDS_ROOTS.length - 1)];
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
