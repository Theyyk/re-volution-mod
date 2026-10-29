package com.example.revolution.balance.config;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.definition.BalanceCurveDefinition;
import com.example.revolution.balance.definition.BalanceCurveMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses the canonical JSON representation into immutable balance definitions. */
public final class BalanceConfigParser {
    private static final int MAX_CURVE_DEPTH = 8;

    public BalanceConfig parse(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) throw new IllegalArgumentException("balance config is empty");
        JsonElement rootElement;
        try { rootElement = new JsonParser().parse(rawJson); }
        catch (RuntimeException e) { throw new IllegalArgumentException("invalid balance JSON", e); }
        if (!rootElement.isJsonObject()) throw new IllegalArgumentException("balance config root must be an object");

        JsonObject root = rootElement.getAsJsonObject();
        int schemaVersion = requiredInt(root, "balanceSchemaVersion", "root");
        Map<String, String> values = parseValues(optionalObject(root, "values", "root"));
        JsonObject curvesObject = requiredObject(root, "curves", "root");
        Map<String, BalanceCurveDefinition> curves = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : curvesObject.entrySet()) {
            if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("curve '" + entry.getKey() + "' must be an object");
            curves.put(entry.getKey(), parseCurve(entry.getValue().getAsJsonObject(), "curves." + entry.getKey(), 0));
        }
        return new BalanceConfig(schemaVersion, values, curves);
    }

    private Map<String, String> parseValues(JsonObject valuesObject) {
        Map<String, String> values = new LinkedHashMap<>();
        if (valuesObject == null) return values;
        for (Map.Entry<String, JsonElement> entry : valuesObject.entrySet()) {
            JsonElement element = entry.getValue();
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("values." + entry.getKey() + " must be a string");
            }
            values.put(entry.getKey(), element.getAsString());
        }
        return values;
    }

    private BalanceCurveDefinition parseCurve(JsonObject object, String path, int depth) {
        if (depth > MAX_CURVE_DEPTH) throw new IllegalArgumentException(path + " exceeds maximum nested curve depth " + MAX_CURVE_DEPTH);
        String rawMode = requiredString(object, "mode", path);
        BalanceCurveMode mode;
        try { mode = BalanceCurveMode.valueOf(rawMode.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException(path + ".mode has unsupported value '" + rawMode + "'"); }

        switch (mode) {
            case FIXED:
                return BalanceCurveDefinition.fixed(requiredDecimal(object, "value", path));
            case TABLE:
                return BalanceCurveDefinition.table(
                        parseTable(requiredArray(object, "values", path), path),
                        optionalDecimal(object, "tail_growth", path));
            case FORMULA:
                return BalanceCurveDefinition.formula(requiredDecimal(object, "base", path), requiredDecimal(object, "growth", path));
            case PIECEWISE:
                return BalanceCurveDefinition.piecewise(parsePieces(requiredArray(object, "segments", path), path, depth + 1));
            default:
                throw new IllegalArgumentException(path + ".mode is unsupported");
        }
    }

    private Map<Integer, BigDecimal> parseTable(JsonArray values, String path) {
        Map<Integer, BigDecimal> table = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) table.put(i, decimal(values.get(i), path + ".values[" + i + "]"));
        return table;
    }

    private List<BalanceCurveDefinition.Piece> parsePieces(JsonArray segments, String path, int depth) {
        List<BalanceCurveDefinition.Piece> pieces = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            String segmentPath = path + ".segments[" + i + "]";
            JsonElement element = segments.get(i);
            if (!element.isJsonObject()) throw new IllegalArgumentException(segmentPath + " must be an object");
            JsonObject segment = element.getAsJsonObject();
            int from = requiredInt(segment, "from", segmentPath);
            Integer to = segment.has("to") && !segment.get("to").isJsonNull() ? integer(segment.get("to"), segmentPath + ".to") : null;
            JsonObject nested = requiredObject(segment, "curve", segmentPath);
            pieces.add(new BalanceCurveDefinition.Piece(from, to, parseCurve(nested, segmentPath + ".curve", depth)));
        }
        return pieces;
    }

    private JsonObject optionalObject(JsonObject object, String field, String path) {
        if (!object.has(field) || object.get(field).isJsonNull()) return null;
        JsonElement element = object.get(field);
        if (!element.isJsonObject()) throw new IllegalArgumentException(path + "." + field + " must be an object");
        return element.getAsJsonObject();
    }

    private BigDecimal optionalDecimal(JsonObject object, String field, String path) {
        if (!object.has(field) || object.get(field).isJsonNull()) return null;
        return decimal(object.get(field), path + "." + field);
    }

    private JsonObject requiredObject(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonObject()) throw new IllegalArgumentException(path + "." + field + " must be an object");
        return element.getAsJsonObject();
    }

    private JsonArray requiredArray(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonArray()) throw new IllegalArgumentException(path + "." + field + " must be an array");
        return element.getAsJsonArray();
    }

    private String requiredString(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(path + "." + field + " must be a string");
        String value = element.getAsString();
        if (value.trim().isEmpty()) throw new IllegalArgumentException(path + "." + field + " must not be blank");
        return value;
    }

    private int requiredInt(JsonObject object, String field, String path) { return integer(required(object, field, path), path + "." + field); }
    private BigDecimal requiredDecimal(JsonObject object, String field, String path) { return decimal(required(object, field, path), path + "." + field); }

    private JsonElement required(JsonObject object, String field, String path) {
        if (!object.has(field) || object.get(field).isJsonNull()) throw new IllegalArgumentException(path + "." + field + " is required");
        return object.get(field);
    }

    private int integer(JsonElement element, String path) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException(path + " must be an integer");
        try { return new BigDecimal(element.getAsString()).intValueExact(); }
        catch (RuntimeException e) { throw new IllegalArgumentException(path + " must be an integer", e); }
    }

    private BigDecimal decimal(JsonElement element, String path) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException(path + " must be a decimal value");
        try { return new BigDecimal(element.getAsString()).stripTrailingZeros(); }
        catch (RuntimeException e) { throw new IllegalArgumentException(path + " must be a decimal value", e); }
    }
}
