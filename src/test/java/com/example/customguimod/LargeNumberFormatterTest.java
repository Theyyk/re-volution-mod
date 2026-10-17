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
        check(LargeNumberValue.unitSuffix(12).equals("UDc"), "Units must continue beyond decillion");
        check(LargeNumberValue.unitSuffix(21).equals("Vg"), "Vigintillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(101).equals("Ce"), "Centillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(102).equals("UCe"), "Uncentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(103).equals("DCe"), "Duocentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(104).equals("TCe"), "Trescentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(105).equals("QaCe"), "Quattuorcentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(106).equals("QiCe"), "Quincentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(107).equals("SxCe"), "Sexcentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(108).equals("SpCe"), "Septencentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(109).equals("OcCe"), "Octocentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(110).equals("NoCe"), "Novemcentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(111).equals("DcCe"), "Decicentillion unit mapping changed");
        check(LargeNumberValue.unitSuffix(411).equals("DcQe"), "1E1233 compact unit mapping changed");
        check(LargeNumberValue.unitCount() == 1001, "Latin-style compact units should extend through the 999th -illion");

        check(LargeNumberFormatter.format(new BigDecimal("1E303")).equals("1Ce"),
                "Centillion should remain compactly formatted");
        check(LargeNumberFormatter.format(new BigDecimal("1E306")).equals("1UCe"),
                "Values above centillion should continue with compact Latin-style suffixes");
        check(LargeNumberFormatter.format(new BigDecimal("1E1233")).equals("1DcQe"),
                "1E1233 should use the extended compact Latin-style suffix");
        check(LargeNumberFormatter.format(new BigDecimal("1E3000")).equals("1NoNogNt"),
                "The extended compact suffix range should reach 1E3000");
        check(LargeNumberFormatter.format(new BigDecimal("1E3003")).equals("1E3003"),
                "Values beyond the configured compact suffix range should fall back to scientific notation");
        check(LargeNumberFormatter.format(LargeNumberValue.parseWithUnit("1000", 11)).equals("1UDc"),
                "1000 Dc should roll over to the next named unit");

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
