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

        check(LargeNumberValue.unitSuffix(18, LargeNumberDisplayMode.LETTER).equals("Spd"), "Septendecillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(19, LargeNumberDisplayMode.LETTER).equals("Ocd"), "Octodecillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(20, LargeNumberDisplayMode.LETTER).equals("Nod"), "Novemdecillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(21, LargeNumberDisplayMode.LETTER).equals("Vg"), "Vigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(22, LargeNumberDisplayMode.LETTER).equals("Uvg"), "Unvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(23, LargeNumberDisplayMode.LETTER).equals("Dvg"), "Duovigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(24, LargeNumberDisplayMode.LETTER).equals("Tvg"), "Trevigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(25, LargeNumberDisplayMode.LETTER).equals("Qavg"), "Quattuorvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(26, LargeNumberDisplayMode.LETTER).equals("Qivg"), "Quinvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(27, LargeNumberDisplayMode.LETTER).equals("Sxvg"), "Sexvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(28, LargeNumberDisplayMode.LETTER).equals("Spvg"), "Septenvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(29, LargeNumberDisplayMode.LETTER).equals("Ocvg"), "Octovigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(30, LargeNumberDisplayMode.LETTER).equals("Nov"), "Novemvigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(31, LargeNumberDisplayMode.LETTER).equals("Tt"), "Trigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(32, LargeNumberDisplayMode.LETTER).equals("Unt"), "Untrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(33, LargeNumberDisplayMode.LETTER).equals("Dtt"), "Duotrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(34, LargeNumberDisplayMode.LETTER).equals("Ttt"), "Tretrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(35, LargeNumberDisplayMode.LETTER).equals("Qut"), "Quattuortrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(36, LargeNumberDisplayMode.LETTER).equals("Qat"), "Quintrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(37, LargeNumberDisplayMode.LETTER).equals("Sst"), "Sextrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(38, LargeNumberDisplayMode.LETTER).equals("Spt"), "Septentrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(39, LargeNumberDisplayMode.LETTER).equals("Oct"), "Octotrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(40, LargeNumberDisplayMode.LETTER).equals("Nvt"), "Novemtrigintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(41, LargeNumberDisplayMode.LETTER).equals("Qdt"), "Quadragintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(51, LargeNumberDisplayMode.LETTER).equals("Qqt"), "Quinquagintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(61, LargeNumberDisplayMode.LETTER).equals("Sxt"), "Sexagintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(71, LargeNumberDisplayMode.LETTER).equals("Sgt"), "Septuagintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(81, LargeNumberDisplayMode.LETTER).equals("Oi"), "Octogintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(91, LargeNumberDisplayMode.LETTER).equals("Noi"), "Nonagintillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(101, LargeNumberDisplayMode.LETTER).equals("C"), "Centillion abbreviation changed");
        check(LargeNumberValue.unitSuffix(121, LargeNumberDisplayMode.LETTER).equals("Vgn"), "Post-centillion viginti abbreviation changed");
        check(LargeNumberValue.unitSuffix(122, LargeNumberDisplayMode.LETTER).equals("Uvgn"), "Post-centillion unviginti abbreviation changed");
        check(LargeNumberValue.unitCount() == 1001, "Letter units should extend through the 999th -illion");

        check(LargeNumberValue.unitSuffix(1, LargeNumberDisplayMode.ENGINEERING).equals("E3"),
                "Engineering selector must start at E3");
        check(LargeNumberValue.unitSuffix(21, LargeNumberDisplayMode.ENGINEERING).equals("E63"),
                "Engineering selector exponent mapping changed");
        check(LargeNumberValue.unitSuffix(101, LargeNumberDisplayMode.ENGINEERING).equals("E303"),
                "Engineering centillion exponent mapping changed");
        check(LargeNumberValue.unitSuffix(445, LargeNumberDisplayMode.ENGINEERING).equals("E1335"),
                "Engineering selector must remain exact at high exponents");

        check(LargeNumberFormatter.format(new BigDecimal("1E90"), LargeNumberDisplayMode.LETTER).equals("1Nov"),
                "1E90 should use Nov in letter mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E93"), LargeNumberDisplayMode.LETTER).equals("1Tt"),
                "1E93 should use Tt in letter mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E120"), LargeNumberDisplayMode.LETTER).equals("1Nvt"),
                "1E120 should use Nvt in letter mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E123"), LargeNumberDisplayMode.LETTER).equals("1Qdt"),
                "1E123 should use Qdt in letter mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E303"), LargeNumberDisplayMode.LETTER).equals("1C"),
                "1E303 should use C in letter mode");
        check(LargeNumberFormatter.format(new BigDecimal("1E363"), LargeNumberDisplayMode.LETTER).equals("1Vgn"),
                "1E363 should use Vgn in letter mode");

        check(LargeNumberFormatter.format(new BigDecimal("1250"), LargeNumberDisplayMode.ENGINEERING).equals("1.25E3"),
                "Engineering mode should use E notation from thousands onward");
        check(LargeNumberFormatter.format(new BigDecimal("1E303"), LargeNumberDisplayMode.ENGINEERING).equals("1E303"),
                "Engineering mode should show centillion as E303");
        check(LargeNumberFormatter.format(new BigDecimal("1E1335"), LargeNumberDisplayMode.ENGINEERING).equals("1E1335"),
                "Engineering mode should preserve high exponents");
        check(LargeNumberFormatter.format(new BigDecimal("1E3003"), LargeNumberDisplayMode.LETTER).equals("1E3003"),
                "Values beyond the configured letter range should fall back to engineering notation");

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
