package com.example.revolution.number;

import com.example.revolution.rune.RuneTestSupport;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class LargeNumberFormatterTest {
    static void run() {
        checkFormat(0, "0");
        checkFormat(12, "12");
        checkFormat(999, "999");
        checkFormat(1000, "1K");
        checkFormat(1250, "1.25K");
        checkFormat(10_000, "10K");
        checkFormat(999_499, "999.5K");
        checkFormat(999_999, "1M");
        checkFormat(1_000_000, "1M");
        checkFormat(1_250_000, "1.25M");
        checkFormat(1_000_000_000L, "1B");
        checkFormat(-1_250_000L, "-1.25M");

        check(LargeNumberFormatter.format(12.345D).equals("12.35"),
                "Plain decimal values should keep at most two decimals");
        check(LargeNumberFormatter.format(40_015.5D).equals("40.02K"),
                "Large rune values should compact without changing the underlying value");
        check(LargeNumberFormatter.format(Double.NaN).equals("0"),
                "Non-finite display values must fail closed to zero");
        check(LargeNumberFormatter.format(Double.POSITIVE_INFINITY).equals("0"),
                "Infinite display values must fail closed to zero");

        BigDecimal preciseMillions = LargeNumberValue.parseWithUnit("1.0912121", 2);
        check(preciseMillions.compareTo(new BigDecimal("1091212.1")) == 0,
                "Unit input must preserve hidden precision for calculations");
        check(LargeNumberFormatter.format(preciseMillions).equals("1.09M"),
                "Unit input should display only two decimal places");

        checkSuffix(18, LargeNumberDisplayMode.SIMPLIFIED, "Spd");
        checkSuffix(19, LargeNumberDisplayMode.SIMPLIFIED, "Ocd");
        checkSuffix(20, LargeNumberDisplayMode.SIMPLIFIED, "Nod");
        checkSuffix(21, LargeNumberDisplayMode.SIMPLIFIED, "Vg");
        checkSuffix(22, LargeNumberDisplayMode.SIMPLIFIED, "Uvg");
        checkSuffix(25, LargeNumberDisplayMode.SIMPLIFIED, "Qavg");
        checkSuffix(31, LargeNumberDisplayMode.SIMPLIFIED, "Tt");
        checkSuffix(32, LargeNumberDisplayMode.SIMPLIFIED, "Unt");
        checkSuffix(41, LargeNumberDisplayMode.SIMPLIFIED, "Qdt");
        checkSuffix(51, LargeNumberDisplayMode.SIMPLIFIED, "Qqt");
        checkSuffix(61, LargeNumberDisplayMode.SIMPLIFIED, "Sxt");
        checkSuffix(71, LargeNumberDisplayMode.SIMPLIFIED, "Sgt");
        checkSuffix(81, LargeNumberDisplayMode.SIMPLIFIED, "Oi");
        checkSuffix(91, LargeNumberDisplayMode.SIMPLIFIED, "Noi");

        checkSuffix(101, LargeNumberDisplayMode.SIMPLIFIED, "Ce");
        checkSuffix(121, LargeNumberDisplayMode.SIMPLIFIED, "Vgc");
        checkSuffix(122, LargeNumberDisplayMode.SIMPLIFIED, "Uvgc");
        checkSuffix(201, LargeNumberDisplayMode.SIMPLIFIED, "Duc");
        checkSuffix(501, LargeNumberDisplayMode.SIMPLIFIED, "Qic");
        checkSuffix(1000, LargeNumberDisplayMode.SIMPLIFIED, "Nvnnc");

        checkSuffix(101, LargeNumberDisplayMode.FULL, "Ce");
        checkSuffix(121, LargeNumberDisplayMode.FULL, "Vigce");
        checkSuffix(122, LargeNumberDisplayMode.FULL, "Unvigce");
        checkSuffix(201, LargeNumberDisplayMode.FULL, "Duce");
        checkSuffix(501, LargeNumberDisplayMode.FULL, "Quing");
        checkSuffix(1000, LargeNumberDisplayMode.FULL, "Novnong");

        check(LargeNumberValue.unitCount() == 1001, "Letter units should extend through E3000");
        checkSuffixCasingAndUniqueness();

        checkSuffix(1, LargeNumberDisplayMode.ENGINEERING, "E3");
        checkSuffix(21, LargeNumberDisplayMode.ENGINEERING, "E63");
        checkSuffix(101, LargeNumberDisplayMode.ENGINEERING, "E303");
        checkSuffix(445, LargeNumberDisplayMode.ENGINEERING, "E1335");

        checkFormat("1E90", LargeNumberDisplayMode.SIMPLIFIED, "1Nov");
        checkFormat("1E93", LargeNumberDisplayMode.SIMPLIFIED, "1Tt");
        checkFormat("1E303", LargeNumberDisplayMode.SIMPLIFIED, "1Ce");
        checkFormat("1E363", LargeNumberDisplayMode.SIMPLIFIED, "1Vgc");
        checkFormat("1E366", LargeNumberDisplayMode.SIMPLIFIED, "1Uvgc");
        checkFormat("1E603", LargeNumberDisplayMode.SIMPLIFIED, "1Duc");
        checkFormat("1E1503", LargeNumberDisplayMode.SIMPLIFIED, "1Qic");
        checkFormat("1E3000", LargeNumberDisplayMode.SIMPLIFIED, "1Nvnnc");

        checkFormat("1E303", LargeNumberDisplayMode.FULL, "1Ce");
        checkFormat("1E363", LargeNumberDisplayMode.FULL, "1Vigce");
        checkFormat("1E366", LargeNumberDisplayMode.FULL, "1Unvigce");
        checkFormat("1E603", LargeNumberDisplayMode.FULL, "1Duce");
        checkFormat("1E1503", LargeNumberDisplayMode.FULL, "1Quing");
        checkFormat("1E3000", LargeNumberDisplayMode.FULL, "1Novnong");

        checkFormat("1250", LargeNumberDisplayMode.ENGINEERING, "1.25E3");
        checkFormat("1E303", LargeNumberDisplayMode.ENGINEERING, "1E303");
        checkFormat("1E1335", LargeNumberDisplayMode.ENGINEERING, "1E1335");
        checkFormat("1E3003", LargeNumberDisplayMode.SIMPLIFIED, "1E3003");
        checkFormat("1E3003", LargeNumberDisplayMode.FULL, "1E3003");

        check(LargeNumberValue.parseFlexible("1Ce").compareTo(new BigDecimal("1E303")) == 0,
                "Current centillion suffix must parse");
        check(LargeNumberValue.parseFlexible("1Vgc").compareTo(new BigDecimal("1E363")) == 0,
                "Simplified extended suffix must parse");
        check(LargeNumberValue.parseFlexible("1Vigce").compareTo(new BigDecimal("1E363")) == 0,
                "Full extended suffix must parse");
        check(LargeNumberValue.parseFlexible("1C").compareTo(new BigDecimal("1E303")) == 0,
                "Legacy centillion suffix must remain readable");
        check(LargeNumberValue.parseFlexible("1Vgn").compareTo(new BigDecimal("1E363")) == 0,
                "Legacy post-centillion suffix must remain readable");

        RuneTestSupport.checkStatFormatting();

        System.out.println("v1.2.11 three-mode large number formatter regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void checkFormat(long value, String expected) {
        check(LargeNumberFormatter.format(value).equals(expected),
                value + " should format as " + expected + " but was " + LargeNumberFormatter.format(value));
    }

    private static void checkFormat(String value, LargeNumberDisplayMode mode, String expected) {
        String actual = LargeNumberFormatter.format(new BigDecimal(value), mode);
        check(actual.equals(expected), value + " in " + mode + " should format as " + expected + " but was " + actual);
    }

    private static void checkSuffix(int index, LargeNumberDisplayMode mode, String expected) {
        String actual = LargeNumberValue.unitSuffix(index, mode);
        check(actual.equals(expected), "Unit " + index + " in " + mode + " should be " + expected + " but was " + actual);
    }

    private static void checkSuffixCasingAndUniqueness() {
        Map<String, Integer> owners = new HashMap<>();
        for (LargeNumberDisplayMode mode : new LargeNumberDisplayMode[]{LargeNumberDisplayMode.SIMPLIFIED, LargeNumberDisplayMode.FULL}) {
            for (int i = 1; i < LargeNumberValue.unitCount(); i++) {
                String suffix = LargeNumberValue.unitSuffix(i, mode);
                check(!suffix.isEmpty(), mode + " suffix " + i + " must not be empty");
                check(Character.isUpperCase(suffix.charAt(0)), mode + " suffix must start uppercase: " + suffix);
                check(suffix.substring(1).equals(suffix.substring(1).toLowerCase(Locale.ROOT)),
                        mode + " suffix must have no internal capitals: " + suffix);
                Integer previous = owners.get(suffix);
                check(previous == null || previous == i,
                        "Suffix " + suffix + " is ambiguous between unit " + previous + " and " + i);
                owners.put(suffix, i);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
