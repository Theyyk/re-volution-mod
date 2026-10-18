package com.example.revolution.number;

/** Test-only access to explicit display modes without exposing the private production settings model. */
public final class NumberTestSupport {
    private NumberTestSupport() {}

    public static String simplifiedSuffix(int index) { return LargeNumberValue.unitSuffix(index, LargeNumberDisplayMode.SIMPLIFIED); }
    public static String fullSuffix(int index) { return LargeNumberValue.unitSuffix(index, LargeNumberDisplayMode.FULL); }
}
