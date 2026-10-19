package com.example.revolution.number;

import java.io.File;
import java.math.BigDecimal;

/** Shared numeric boundary; implementation and display-mode state remain package-private. */
public final class LargeNumbers {
    private LargeNumbers() {}

    public static void initDisplaySettings(File directory) { LargeNumberDisplaySettings.init(directory); }
    public static String format(long value) { return LargeNumberFormatter.format(value); }
    public static String format(double value) { return LargeNumberFormatter.format(value); }
    public static String format(BigDecimal value) { return LargeNumberFormatter.format(value); }
    public static int unitCount() { return LargeNumberValue.unitCount(); }
    public static String unitSuffix(int index) { return LargeNumberValue.unitSuffix(index); }
    public static BigDecimal parseWithUnit(String raw, int index) { return LargeNumberValue.parseWithUnit(raw, index); }
    public static BigDecimal parseFlexible(String raw) { return LargeNumberValue.parseFlexible(raw); }
    public static BigDecimal parseExact(String raw) { return LargeNumberValue.parseExact(raw); }
    public static String canonical(BigDecimal value) { return LargeNumberValue.canonical(value); }
    public static BigDecimal normalize(BigDecimal value) { return LargeNumberValue.normalize(value); }

    public static LargeNumberDisplayMode displayMode() { return LargeNumberDisplaySettings.getMode(); }
    public static void setDisplayMode(LargeNumberDisplayMode mode) { LargeNumberDisplaySettings.setMode(mode); }
}
