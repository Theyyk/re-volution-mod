package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression coverage for the v1 spreadsheet-friendly Balance Core export contract. */
public final class BalanceExportServiceTest {
    @Test
    void exportsActiveSnapshotDeterministically() {
        BalanceConfigCodec codec = new BalanceConfigCodec();
        BalanceService balance = service(codec);
        BalanceExportService export = new BalanceExportService(balance, codec);

        expectFailure(export::exportActive, "export without an active snapshot must fail");

        String source = "{\"balanceSchemaVersion\":1,\"values\":{"
                + "\"runes.poison.purchase.resource\":\"coins\","
                + "\"runes.poison.purchase.amount\":\"100\"},\"curves\":{"
                + "\"runes.poison.enhancement.cost\":{\"mode\":\"FORMULA\",\"base\":\"10.00\",\"growth\":\"1.50\"},"
                + "\"sample.table\":{\"mode\":\"TABLE\",\"values\":[\"1\",\"2.0\"]}}}";
        BalanceSnapshot active = balance.activate(source);
        BalanceExportDocument first = export.exportActive();

        check(first.getExportContractVersion() == 1, "export contract version must be explicit");
        check(first.getRevision().equals(active.getRevision()), "export revision must identify the active snapshot");
        check(first.getBalanceSchemaVersion() == 1, "balance schema version must be exported");
        check(first.getCanonicalConfig().equals(codec.canonicalJson(active.getConfig())),
                "canonical config must remain the authoritative payload");
        check(first.getColumns().equals(java.util.Arrays.asList("pointer", "type", "value")),
                "spreadsheet columns must be stable");
        check(row(first.getRows(), "/balanceSchemaVersion").getType().equals("number"),
                "JSON numeric metadata must retain its type");
        check(row(first.getRows(), "/values/runes.poison.purchase.amount").getValue().equals("100"),
                "scalar balance values must be exported without numeric narrowing");
        check(row(first.getRows(), "/curves/runes.poison.enhancement.cost/base").getValue().equals("10"),
                "canonical decimal formatting must be used in rows");
        check(row(first.getRows(), "/curves/sample.table/values/1").getValue().equals("2"),
                "table leaves must use deterministic array pointers");

        String reordered = "{\"curves\":{"
                + "\"sample.table\":{\"values\":[\"1.0\",\"2\"],\"mode\":\"TABLE\"},"
                + "\"runes.poison.enhancement.cost\":{\"growth\":\"1.5\",\"base\":\"10\",\"mode\":\"FORMULA\"}},"
                + "\"values\":{\"runes.poison.purchase.amount\":\"100\",\"runes.poison.purchase.resource\":\"coins\"},"
                + "\"balanceSchemaVersion\":1}";
        BalanceSnapshot equivalent = balance.prepare(reordered);
        BalanceExportDocument second = export.export(equivalent);
        check(first.getRevision().equals(second.getRevision()), "equivalent balance configs must keep the same revision");
        check(first.getCanonicalConfig().equals(second.getCanonicalConfig()),
                "equivalent balance configs must export the same canonical config");
        checkRowsEqual(first.getRows(), second.getRows());
    }

    @Test
    void escapesJsonPointerTokens() {
        BalanceConfigCodec codec = new BalanceConfigCodec();
        BalanceService balance = service(codec);
        BalanceExportService export = new BalanceExportService(balance, codec);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("external/a~b", "value");
        BalanceSnapshot snapshot = new BalanceSnapshot(
                new BalanceConfig(1, values, Collections.emptyMap()),
                "test-revision");

        BalanceExportDocument document = export.export(snapshot);
        check(row(document.getRows(), "/values/external~1a~0b").getValue().equals("value"),
                "JSON Pointer tokens must escape slash and tilde without changing balance ids");
    }

    private static BalanceService service(BalanceConfigCodec codec) {
        return new BalanceService(
                new BalanceConfigParser(),
                new BalanceConfigValidator(),
                codec,
                new BalanceCurveEngine());
    }

    private static BalanceExportDocument.Row row(List<BalanceExportDocument.Row> rows, String pointer) {
        for (BalanceExportDocument.Row row : rows) {
            if (pointer.equals(row.getPointer())) return row;
        }
        throw new AssertionError("missing export row " + pointer);
    }

    private static void checkRowsEqual(List<BalanceExportDocument.Row> left, List<BalanceExportDocument.Row> right) {
        check(left.size() == right.size(), "equivalent exports must have the same row count");
        for (int i = 0; i < left.size(); i++) {
            BalanceExportDocument.Row a = left.get(i);
            BalanceExportDocument.Row b = right.get(i);
            check(a.getPointer().equals(b.getPointer()), "row pointers must be deterministic at index " + i);
            check(a.getType().equals(b.getType()), "row types must be deterministic at index " + i);
            check(a.getValue() == null ? b.getValue() == null : a.getValue().equals(b.getValue()),
                    "row values must be deterministic at index " + i);
        }
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
