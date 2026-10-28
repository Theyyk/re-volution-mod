package com.example.revolution.balance.config;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import org.junit.jupiter.api.Test;

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

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected parser failure");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
