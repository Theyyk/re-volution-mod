package com.example.revolution.balance.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable v1 machine-readable export of one active Balance Core snapshot. */
public final class BalanceExportDocument {
    public static final int EXPORT_CONTRACT_VERSION = 1;

    private final int exportContractVersion;
    private final String revision;
    private final int balanceSchemaVersion;
    private final String canonicalConfig;
    private final List<String> columns;
    private final List<Row> rows;

    public BalanceExportDocument(
            String revision,
            int balanceSchemaVersion,
            String canonicalConfig,
            List<Row> rows) {
        if (revision == null || revision.trim().isEmpty()) throw new IllegalArgumentException("revision is required");
        if (canonicalConfig == null || canonicalConfig.trim().isEmpty()) throw new IllegalArgumentException("canonical config is required");
        if (rows == null) throw new IllegalArgumentException("rows are required");
        this.exportContractVersion = EXPORT_CONTRACT_VERSION;
        this.revision = revision;
        this.balanceSchemaVersion = balanceSchemaVersion;
        this.canonicalConfig = canonicalConfig;
        this.columns = Collections.unmodifiableList(java.util.Arrays.asList("pointer", "type", "value"));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public int getExportContractVersion() {
        return exportContractVersion;
    }

    public String getRevision() {
        return revision;
    }

    public int getBalanceSchemaVersion() {
        return balanceSchemaVersion;
    }

    public String getCanonicalConfig() {
        return canonicalConfig;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    /** One spreadsheet-friendly leaf from canonical JSON, addressed by RFC 6901 JSON Pointer. */
    public static final class Row {
        private final String pointer;
        private final String type;
        private final String value;

        public Row(String pointer, String type, String value) {
            if (pointer == null) throw new IllegalArgumentException("pointer is required");
            if (type == null || type.trim().isEmpty()) throw new IllegalArgumentException("type is required");
            this.pointer = pointer;
            this.type = type;
            this.value = value;
        }

        public String getPointer() {
            return pointer;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
