package com.example.revolution.admin.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AdminApiInputValidationTest {
    public static void main(String[] args) {
        testFormParser();
        testExactNumbers();
        testCoordinates();
        System.out.println("Admin API form, exact-number and coordinate validation passed");
    }

    private static void testFormParser() {
        Set<String> expected = new HashSet<>(Arrays.asList("uuid", "resource", "value"));
        AdminFormData form = AdminFormData.parse(
                "uuid=00000000-0000-0000-0000-000000000001&resource=rune_dust&value=1E3003",
                expected);
        require("rune_dust".equals(form.get("resource")), "resource parse failed");
        require("1E3003".equals(form.get("value")), "value parse failed");

        reject(() -> AdminFormData.parse("uuid=x&resource=coins", expected), "missing field accepted");
        reject(() -> AdminFormData.parse("uuid=x&resource=coins&value=1&extra=2", expected), "unknown field accepted");
        reject(() -> AdminFormData.parse("uuid=x&resource=coins&value=1&value=2", expected), "duplicate field accepted");
        reject(() -> AdminFormData.parse("bad%ZZ=value", Collections.singleton("bad")), "invalid percent encoding accepted");
    }

    private static void testExactNumbers() {
        BigDecimal huge = AdminApiRoutes.exactNumber("1E3003", false);
        require(huge.compareTo(new BigDecimal("1E3003")) == 0, "large exact number rejected");
        require(AdminApiRoutes.exactNumber("-5", true).compareTo(BigDecimal.valueOf(-5)) == 0, "signed delta rejected");
        reject(() -> AdminApiRoutes.exactNumber("-1", false), "negative absolute value accepted");
        reject(() -> AdminApiRoutes.exactNumber("1E10001", false), "unbounded exponent accepted");
        reject(() -> AdminApiRoutes.exactNumber(repeat('9', 101), false), "excessive precision accepted");
    }

    private static void testCoordinates() {
        require(AdminApiRoutes.coordinate("30000000", false) == 30000000.0D, "world border coordinate rejected");
        require(AdminApiRoutes.coordinate("-4096", true) == -4096.0D, "vertical bound rejected");
        reject(() -> AdminApiRoutes.coordinate("30000001", false), "horizontal overflow accepted");
        reject(() -> AdminApiRoutes.coordinate("4097", true), "vertical overflow accepted");
        reject(() -> AdminApiRoutes.coordinate("NaN", false), "NaN accepted");
        reject(() -> AdminApiRoutes.coordinate("Infinity", false), "infinity accepted");
    }

    private static String repeat(char c, int count) {
        StringBuilder out = new StringBuilder(count);
        for (int i = 0; i < count; i++) out.append(c);
        return out.toString();
    }

    private static void reject(ThrowingRunnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        } catch (Exception e) {
            throw new AssertionError(message, e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface ThrowingRunnable { void run() throws Exception; }
}
