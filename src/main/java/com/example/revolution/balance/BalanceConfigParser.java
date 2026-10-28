package com.example.revolution.balance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict JSON parser for the canonical balance document. */
public final class BalanceConfigParser {
    public BalanceConfig parse(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw new IllegalArgumentException("balance config is empty");
        }

        JsonElement parsed;
        try {
            parsed = new JsonParser().parse(rawJson);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid balance JSON", e);
        }
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("balance root must be an object");

        JsonObject root = parsed.getAsJsonObject();
        rejectUnknown(root, "balanceSchemaVersion", "curves");
        int schemaVersion = requiredInt(root, "balanceSchemaVersion");
        JsonObject curvesObject = requiredObject(root, "curves");

        Map<String, BalanceCurve> curves = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : curvesObject.entrySet()) {
            String id = entry.getKey();
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("curve id must not be blank");
            if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("curve '" + id + "' must be an object");
            curves.put(id, parseCurve(id, entry.getValue().getAsJsonObject()));
        }
        return new BalanceConfig(schemaVersion, curves);
    }

    private BalanceCurve parseCurve(String path, JsonObject object) {
        String modeRaw = requiredString(object, "mode");
        BalanceCurve.Mode mode;
        try {
            mode = BalanceCurve.Mode.valueOf(modeRaw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("curve '" + path + "' has unsupported mode: " + modeRaw);
        }

        switch (mode) {
            case FIXED:
                rejectUnknown(object, "mode", "value");
                return BalanceCurve.fixed(requiredDecimal(object, "value"));
            case TABLE:
                rejectUnknown(object, "mode", "values");
                return BalanceCurve.table(parseTable(path, requiredObject(object, "values")));
            case FORMULA:
                rejectUnknown(object, "mode", "base", "growth");
                return BalanceCurve.formula(requiredDecimal(object, "base"), requiredDecimal(object, "growth"));
            case PIECEWISE:
                rejectUnknown(object, "mode", "segments");
                return BalanceCurve.piecewise(parseSegments(path, requiredArray(object, "segments")));
            default:
                throw new IllegalArgumentException("unsupported curve mode: " + mode);
        }
    }

    private Map<Integer, BigDecimal> parseTable(String path, JsonObject values) {
        Map<Integer, BigDecimal> table = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            final int index;
            try {
                index = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("curve '" + path + "' table index must be an integer: " + entry.getKey());
            }
            if (table.containsKey(index)) throw new IllegalArgumentException("curve '" + path + "' contains duplicate table index " + index);
            table.put(index, decimal(entry.getValue(), "curve '" + path + "' table value " + index));
        }
        return table;
    }

    private List<BalanceCurve.Segment> parseSegments(String path, JsonArray array) {
        List<BalanceCurve.Segment> segments = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) throw new IllegalArgumentException("curve '" + path + "' segment " + i + " must be an object");
            JsonObject segment = element.getAsJsonObject();
            rejectUnknown(segment, "from", "to", "curve");
            int from = requiredInt(segment, "from");
            int to = requiredInt(segment, "to");
            JsonObject nested = requiredObject(segment, "curve");
            segments.add(new BalanceCurve.Segment(from, to, parseCurve(path + ".segment[" + i + "]", nested)));
        }
        return segments;
    }

    private static JsonObject requiredObject(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("field '" + field + "' must be an object");
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonArray()) throw new IllegalArgumentException("field '" + field + "' must be an array");
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) throw new IllegalArgumentException("field '" + field + "' must be a string");
        return value.getAsString();
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) throw new IllegalArgumentException("field '" + field + "' must be an integer");
        try {
            return value.getAsInt();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("field '" + field + "' must be an integer", e);
        }
    }

    private static BigDecimal requiredDecimal(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null) throw new IllegalArgumentException("missing field '" + field + "'");
        return decimal(value, "field '" + field + "'");
    }

    private static BigDecimal decimal(JsonElement value, String label) {
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) throw new IllegalArgumentException(label + " must be a decimal value");
        try {
            return new BigDecimal(value.getAsString()).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a finite decimal value", e);
        }
    }

    private static void rejectUnknown(JsonObject object, String... allowed) {
        java.util.Set<String> names = new java.util.HashSet<>();
        java.util.Collections.addAll(names, allowed);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!names.contains(entry.getKey())) throw new IllegalArgumentException("unknown field '" + entry.getKey() + "'");
        }
    }
}
