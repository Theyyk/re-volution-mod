package com.example.customguimod;

import java.math.BigDecimal;

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

        check(LargeNumberValue.unitSuffix(11).equals("Dc"), "Decillion unit index changed");
        check(LargeNumberValue.unitSuffix(12).equals("Ud"), "Units must continue beyond decillion");
        check(LargeNumberValue.unitSuffix(21).equals("Vg"), "Vigintillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(101).equals("Ce"), "Centillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(102).equals("CeA"), "Post-centillion units must use letters only");
        check(LargeNumberValue.unitSuffix(127).equals("CeZ"), "Alphabetic post-centillion boundary changed");
        check(LargeNumberValue.unitSuffix(128).equals("CeAA"), "Alphabetic post-centillion rollover changed");
        check(LargeNumberValue.unitCount() > 411, "Extended unit list must cover exponent 1233");
        check(LargeNumberValue.unitSuffix(411).equals("CeKX"), "1E1233 unit mapping changed");
        check(LargeNumberFormatter.format(new BigDecimal("1E1233")).equals("1CeKX"),
                "1E1233 should stay in letters-only compact unit format");
        check(LargeNumberFormatter.format(LargeNumberValue.parseWithUnit("1000", 11)).equals("1Ud"),
                "1000 Dc should roll over to the next named unit instead of scientific notation");
        check(LargeNumberFormatter.format(LargeNumberValue.parseWithUnit("1", 101)).equals("1Ce"),
                "Centillion should remain compactly formatted");

        check(RuneStatType.CLICK_DAMAGE_FLAT.format(40_015.5D).equals("+40.02K"),
                "Rune stat display should use the shared large number formatter");
        check(RuneStatType.POISON_DAMAGE_MULTIPLIER.format(3.0D).equals("x3"),
                "Small multiplier formatting must stay unchanged");

        System.out.println("v1.2.11 large number formatter regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void checkFormat(long value, String expected) {
        check(LargeNumberFormatter.format(value).equals(expected),
                value + " should format as " + expected + " but was " + LargeNumberFormatter.format(value));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
