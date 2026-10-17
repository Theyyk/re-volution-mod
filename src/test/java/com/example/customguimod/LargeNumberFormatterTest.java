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
        check(LargeNumberValue.unitSuffix(12).equals("Ud"), "Standard undecillion suffix changed");
        check(LargeNumberValue.unitSuffix(13).equals("Dd"), "Standard duodecillion suffix changed");
        check(LargeNumberValue.unitSuffix(21).equals("Vg"), "Vigintillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(22).equals("UVg"), "Standard post-vigintillion suffix changed");
        check(LargeNumberValue.unitSuffix(101).equals("Ce"), "Centillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(102).equals("UCe"), "Custom continuation should start after centillion");
        check(LargeNumberValue.unitSuffix(190).equals("NoOcgCe"), "1E570 full suffix mapping changed");
        check(LargeNumberValue.unitSuffix(412).equals("UDcQe"), "1E1236 full suffix mapping changed");
        check(LargeNumberValue.unitSuffix(445).equals("QaQagQe"), "1E1335 full suffix mapping changed");
        check(LargeNumberValue.unitCount() == 1001, "Compact units should extend through the 999th -illion");

        check(LargeNumberValue.unitSuffix(190, LargeNumberDisplayMode.ABBREVIATED).equals("NOCe"),
                "1E570 abbreviated suffix mapping changed");
        check(LargeNumberValue.unitSuffix(412, LargeNumberDisplayMode.ABBREVIATED).equals("UDQe"),
                "1E1236 abbreviated suffix mapping changed");
        check(LargeNumberValue.unitSuffix(445, LargeNumberDisplayMode.ABBREVIATED).equals("QQQe"),
                "1E1335 abbreviated suffix mapping changed");
        check(LargeNumberValue.unitSuffix(190, LargeNumberDisplayMode.SHORT).equals("NOC"),
                "1E570 short suffix mapping changed");
        check(LargeNumberValue.unitSuffix(412, LargeNumberDisplayMode.SHORT).equals("UDQ"),
                "1E1236 short suffix mapping changed");
        check(LargeNumberValue.unitSuffix(445, LargeNumberDisplayMode.SHORT).equals("QQQ"),
                "1E1335 short suffix mapping changed");

        check(LargeNumberFormatter.format(new BigDecimal("1E303"), LargeNumberDisplayMode.SHORT).equals("1Ce"),
                "Values through centillion must ignore the post-Ce display mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E570"), LargeNumberDisplayMode.FULL).equals("1NoOcgCe"),
                "Full mode should use compositional suffixes above centillion");
        check(LargeNumberFormatter.format(new BigDecimal("1E570"), LargeNumberDisplayMode.ABBREVIATED).equals("1NOCe"),
                "Abbreviated mode should shorten compositional suffixes");
        check(LargeNumberFormatter.format(new BigDecimal("1E570"), LargeNumberDisplayMode.SHORT).equals("1NOC"),
                "Short mode should use three-letter compositional suffixes");
        check(LargeNumberFormatter.format(new BigDecimal("1E570"), LargeNumberDisplayMode.SCIENTIFIC).equals("1E570"),
                "Scientific mode should switch only values above centillion to E notation");
        check(LargeNumberFormatter.format(new BigDecimal("1E1335"), LargeNumberDisplayMode.FULL).equals("1QaQagQe"),
                "1E1335 full mode mapping changed");
        check(LargeNumberFormatter.format(new BigDecimal("1E1335"), LargeNumberDisplayMode.ABBREVIATED).equals("1QQQe"),
                "1E1335 abbreviated mode mapping changed");
        check(LargeNumberFormatter.format(new BigDecimal("1E1335"), LargeNumberDisplayMode.SHORT).equals("1QQQ"),
                "1E1335 short mode mapping changed");
        check(LargeNumberFormatter.format(new BigDecimal("1E1335"), LargeNumberDisplayMode.SCIENTIFIC).equals("1E1335"),
                "1E1335 scientific mode mapping changed");
        check(LargeNumberFormatter.format(new BigDecimal("1E3000"), LargeNumberDisplayMode.FULL).equals("1NoNogNt"),
                "The extended compact suffix range should reach 1E3000");
        check(LargeNumberFormatter.format(new BigDecimal("1E3003"), LargeNumberDisplayMode.FULL).equals("1E3003"),
                "Values beyond the configured compact suffix range should fall back to scientific notation");
        check(LargeNumberFormatter.format(LargeNumberValue.parseWithUnit("1000", 11)).equals("1Ud"),
                "1000 Dc should roll over to the standard next named unit");

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
