package com.example.revolution.balance.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only deterministic export projection over the active Balance Core snapshot. */
public final class BalanceExportService {
    private final BalanceService balance;
    private final BalanceConfigCodec codec;

    public BalanceExportService(BalanceService balance, BalanceConfigCodec codec) {
        if (balance == null || codec == null) throw new IllegalArgumentException("balance export dependencies are required");
        this.balance = balance;
        this.codec = codec;
    }

    public BalanceExportDocument exportActive() {
        BalanceSnapshot snapshot = balance.getActiveSnapshot();
        if (snapshot == null) throw new IllegalStateException("no active balance config");
        return export(snapshot);
    }

    public BalanceExportDocument export(BalanceSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("balance snapshot is required");
        String canonical = codec.canonicalJson(snapshot.getConfig());
        JsonElement root = new JsonParser().parse(canonical);
        List<BalanceExportDocument.Row> rows = new ArrayList<>();
        flatten(root, "", rows);
        return new BalanceExportDocument(
                snapshot.getRevision(),
                snapshot.getConfig().getBalanceSchemaVersion(),
                canonical,
                rows);
    }

    private void flatten(JsonElement element, String pointer, List<BalanceExportDocument.Row> rows) {
        if (element == null || element.isJsonNull()) {
            rows.add(new BalanceExportDocument.Row(pointer, "null", null));
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                flatten(entry.getValue(), pointer + "/" + escapePointerToken(entry.getKey()), rows);
            }
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                flatten(array.get(i), pointer + "/" + i, rows);
            }
            return;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            rows.add(new BalanceExportDocument.Row(pointer, "boolean", Boolean.toString(primitive.getAsBoolean())));
        } else if (primitive.isNumber()) {
            rows.add(new BalanceExportDocument.Row(pointer, "number", primitive.getAsString()));
        } else {
            rows.add(new BalanceExportDocument.Row(pointer, "string", primitive.getAsString()));
        }
    }

    private String escapePointerToken(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }
}
