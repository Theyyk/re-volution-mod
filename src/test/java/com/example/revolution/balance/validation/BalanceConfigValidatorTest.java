package com.example.revolution.balance.validation;

import com.example.revolution.balance.config.BalanceConfigParser;
import org.junit.jupiter.api.Test;

/** Focused semantic validation regression cases. */
public final class BalanceConfigValidatorTest {
    @Test
    void rejectsGapsNegativeValuesAndBadIds() {
        BalanceConfigParser parser = new BalanceConfigParser();
        BalanceConfigValidator validator = new BalanceConfigValidator();

        expectValidationFailure(validator, parser,
                "{\"balanceSchemaVersion\":1,\"curves\":{\"Bad ID\":{\"mode\":\"FIXED\",\"value\":1}}}");
        expectValidationFailure(validator, parser,
                "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":-1}}}");
        expectValidationFailure(validator, parser,
                "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"TABLE\",\"values\":[]}}}");
        expectValidationFailure(validator, parser,
                "{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"PIECEWISE\",\"segments\":["
                        + "{\"from\":0,\"to\":1,\"curve\":{\"mode\":\"FIXED\",\"value\":1}},"
                        + "{\"from\":3,\"curve\":{\"mode\":\"FIXED\",\"value\":1}}]}}}");
    }

    private static void expectValidationFailure(
            BalanceConfigValidator validator,
            BalanceConfigParser parser,
            String json) {
        try {
            validator.validate(parser.parse(json));
            throw new AssertionError("expected validation failure");
        } catch (BalanceValidationException expected) {
            if (expected.getErrors().isEmpty()) throw new AssertionError("validation failure must contain errors");
        }
    }
}
