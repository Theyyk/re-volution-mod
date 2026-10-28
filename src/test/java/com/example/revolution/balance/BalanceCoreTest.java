package com.example.revolution.balance;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.balance.validation.BalanceValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pure Java regression coverage for the v1 balance core. */
public final class BalanceCoreTest {
    public static void main(String[] args) {
        run();
        System.out.println("Balance core parser, validation, curves, revision and atomic activation checks passed");
    }

    @Test
    void balanceCoreRegression() {
        run();
    }

    public static void run() {
        BalanceConfigParser parser = new BalanceConfigParser();
        BalanceConfigValidator validator = new BalanceConfigValidator();
        BalanceConfigCodec codec = new BalanceConfigCodec();
        BalanceCurveEngine engine = new BalanceCurveEngine();
        BalanceService service = new BalanceService(parser, validator, codec, engine);

        String valid = "{\n"
                + "  \"balanceSchemaVersion\": 1,\n"
                + "  \"curves\": {\n"
                + "    \"fixed\": {\"mode\":\"FIXED\",\"value\":\"100\"},\n"
                + "    \"table\": {\"mode\":\"TABLE\",\"values\":[\"100\",\"120\",\"150\"]},\n"
                + "    \"formula\": {\"mode\":\"FORMULA\",\"base\":\"10\",\"growth\":\"1.5\"},\n"
                + "    \"huge\": {\"mode\":\"FIXED\",\"value\":\"100000000000000000000000000000000000000000000000000\"},\n"
                + "    \"piecewise\": {\"mode\":\"PIECEWISE\",\"segments\":[\n"
                + "      {\"from\":0,\"to\":1,\"curve\":{\"mode\":\"TABLE\",\"values\":[\"5\",\"7\"]}},\n"
                + "      {\"from\":2,\"curve\":{\"mode\":\"FORMULA\",\"base\":\"10\",\"growth\":\"2\"}}\n"
                + "    ]}\n"
                + "  }\n"
                + "}";

        BalanceSnapshot snapshot = service.activate(valid);
        check(snapshot.getConfig().getBalanceSchemaVersion() == 1, "schema version must be parsed");
        checkDecimal("100", service.evaluateActiveCurve("fixed", 99), "FIXED must ignore index");
        checkDecimal("120", service.evaluateActiveCurve("table", 1), "TABLE must use explicit index");
        checkDecimal("33.75", service.evaluateActiveCurve("formula", 3), "FORMULA must evaluate base * growth^index");
        checkDecimal("5", service.evaluateActiveCurve("piecewise", 0), "first PIECEWISE segment must use local TABLE index 0");
        checkDecimal("7", service.evaluateActiveCurve("piecewise", 1), "first PIECEWISE segment must use local TABLE index 1");
        checkDecimal("10", service.evaluateActiveCurve("piecewise", 2), "second PIECEWISE segment must restart local FORMULA index at 0");
        checkDecimal("40", service.evaluateActiveCurve("piecewise", 4), "PIECEWISE FORMULA must advance from local segment index");
        checkDecimal("100000000000000000000000000000000000000000000000000",
                service.evaluateActiveCurve("huge", 0), "large values must not be narrowed to int/long/double");

        String sameDifferentOrder = "{\"curves\":{"
                + "\"piecewise\":{\"segments\":[{\"curve\":{\"values\":[\"5\",\"7\"],\"mode\":\"TABLE\"},\"to\":1,\"from\":0},{\"curve\":{\"growth\":\"2.0\",\"base\":\"10.0\",\"mode\":\"FORMULA\"},\"from\":2}],\"mode\":\"PIECEWISE\"},"
                + "\"huge\":{\"value\":\"100000000000000000000000000000000000000000000000000.0\",\"mode\":\"FIXED\"},"
                + "\"formula\":{\"growth\":\"1.50\",\"base\":\"10.00\",\"mode\":\"FORMULA\"},"
                + "\"table\":{\"values\":[\"100.0\",\"120.00\",\"150\"],\"mode\":\"TABLE\"},"
                + "\"fixed\":{\"value\":\"100.0\",\"mode\":\"FIXED\"}},\"balanceSchemaVersion\":1}";
        BalanceSnapshot reordered = service.prepare(sameDifferentOrder);
        check(snapshot.getRevision().equals(reordered.getRevision()),
                "revision must be based on canonical semantic content, not JSON property order or decimal formatting");
        check(snapshot.getRevision().matches("[0-9a-f]{64}"), "revision must be lowercase SHA-256");

        expectFailure(() -> engine.evaluate(snapshot.getConfig().getCurve("table"), 3),
                "TABLE lookup beyond explicit values must fail instead of silently clamping");
        expectFailure(() -> service.evaluateActiveCurve("missing", 0),
                "unknown active curve must fail explicitly");

        String invalid = "{\"balanceSchemaVersion\":2,\"curves\":{"
                + "\"Bad ID\":{\"mode\":\"FIXED\",\"value\":\"-1\"},"
                + "\"broken_piece\":{\"mode\":\"PIECEWISE\",\"segments\":["
                + "{\"from\":1,\"to\":0,\"curve\":{\"mode\":\"FIXED\",\"value\":\"1\"}}]}}}";

        BalanceConfig invalidConfig = parser.parse(invalid);
        try {
            validator.validate(invalidConfig);
            throw new AssertionError("invalid config must fail validation");
        } catch (BalanceValidationException e) {
            check(e.getErrors().size() >= 4, "validator must aggregate independent semantic errors");
        }

        String activeRevisionBeforeFailure = service.getActiveSnapshot().getRevision();
        expectFailure(() -> service.activate(invalid), "invalid activation must fail");
        check(activeRevisionBeforeFailure.equals(service.getActiveSnapshot().getRevision()),
                "failed activation must preserve the previous active snapshot");
        checkDecimal("120", service.evaluateActiveCurve("table", 1),
                "previous active config must remain usable after failed activation");

        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"UNKNOWN\"}}}"),
                "unknown curve mode must fail parsing");
        expectFailure(() -> parser.parse("{\"balanceSchemaVersion\":1,\"curves\":{\"x\":{\"mode\":\"FIXED\",\"value\":\"NaN\"}}}"),
                "NaN must not enter the BigDecimal balance model");
    }

    private static void checkDecimal(String expected, BigDecimal actual, String message) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual.toPlainString());
        }
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected failure is the assertion.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
