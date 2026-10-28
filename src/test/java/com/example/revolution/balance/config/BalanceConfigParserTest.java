package com.example.revolution.balance.config;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Focused parser/validator edge cases kept separate from service lifecycle coverage. */
public final class BalanceConfigParserTest {
    @Test
    void acceptsNumericPrimitivesAndRejectsMalformedStructure() {
        BalanceConfigParser parser = new BalanceConfigParser();
        BalanceConfigValidator validator = new BalanceConfigValidator();

        BalanceConfig numeric = parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{"
                + "\"cost\":{\"mode\":\"FORMULA\",\"base\":100,\"growth\":1.25}}}");
        validator.validate(numeric);

        expectFailure(() -> parser.parse("[]"));
        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":[]}"));
        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{\"x\":5}}"));
        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"TABLE\",\"values\":{}}}}"));
        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"PIECEWISE\",\"segments\":[5]}}}"));
    }

    @Test
    void parsesOptionalStringValuesAndIncludesThemInCanonicalRevision() {
        BalanceConfigParser parser = new BalanceConfigParser();
        BalanceConfigValidator validator = new BalanceConfigValidator();
        BalanceConfigCodec codec = new BalanceConfigCodec();

        BalanceConfig first = parser.parse("{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\"},\"curves\":{"
                + "\"cost\":{\"mode\":\"FIXED\",\"value\":\"100\"}}}");
        BalanceConfig second = parser.parse("{\"curves\":{\"cost\":{\"value\":100.0,\"mode\":\"FIXED\"}},"
                + "\"values\":{\"runes.poison.purchase.resource\":\"coins\"},\"balanceSchemaVersion\":1}");
        validator.validate(first);
        validator.validate(second);

        assertEquals("coins", first.getValue("runes.poison.purchase.resource"));
        assertEquals(codec.revision(first), codec.revision(second));

        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"values\":{\"x\":5},\"curves\":{"
                + "\"cost\":{\"mode\":\"FIXED\",\"value\":\"100\"}}}"));
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected parser failure");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
