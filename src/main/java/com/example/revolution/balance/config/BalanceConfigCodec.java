package com.example.revolution.balance.config;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.balance.definition.BalanceCurveDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Deterministic canonical JSON and SHA-256 revision for validated balance configs. */
public final class BalanceConfigCodec {
    public String canonicalJson(BalanceConfig config) {
        if (config == null) throw new IllegalArgumentException("config is required");
        JsonObject root = new JsonObject();
        root.addProperty("balanceSchemaVersion", config.getBalanceSchemaVersion());

        if (!config.getValues().isEmpty()) {
            JsonObject values = new JsonObject();
            List<String> valueIds = new ArrayList<>(config.getValues().keySet());
            Collections.sort(valueIds);
            for (String id : valueIds) values.addProperty(id, config.getValues().get(id));
            root.add("values", values);
        }

        JsonObject curves = new JsonObject();
        List<String> ids = new ArrayList<>(config.getCurves().keySet());
        Collections.sort(ids);
        for (String id : ids) curves.add(id, writeCurve(config.getCurves().get(id)));
        root.add("curves", curves);
        return root.toString();
    }

    public String revision(BalanceConfig config) {
        byte[] bytes = canonicalJson(config).getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte value : hash) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private JsonObject writeCurve(BalanceCurveDefinition curve) {
        if (curve == null || curve.getMode() == null) throw new IllegalArgumentException("curve and mode are required");
        JsonObject object = new JsonObject();
        object.addProperty("mode", curve.getMode().name());
        switch (curve.getMode()) {
            case FIXED:
                object.addProperty("value", canonical(curve.getFixedValue()));
                break;
            case TABLE:
                JsonArray values = new JsonArray();
                for (Map.Entry<Integer, BigDecimal> entry : curve.getTableValues().entrySet()) values.add(canonical(entry.getValue()));
                object.add("values", values);
                if (curve.getTableTailGrowth() != null) object.addProperty("tail_growth", canonical(curve.getTableTailGrowth()));
                break;
            case FORMULA:
                object.addProperty("base", canonical(curve.getFormulaBase()));
                object.addProperty("growth", canonical(curve.getFormulaGrowth()));
                break;
            case PIECEWISE:
                JsonArray segments = new JsonArray();
                for (BalanceCurveDefinition.Piece piece : curve.getPieces()) {
                    JsonObject segment = new JsonObject();
                    segment.addProperty("from", piece.getFrom());
                    if (piece.getTo() != null) segment.addProperty("to", piece.getTo());
                    segment.add("curve", writeCurve(piece.getCurve()));
                    segments.add(segment);
                }
                object.add("segments", segments);
                break;
            default:
                throw new IllegalArgumentException("unsupported curve mode " + curve.getMode());
        }
        return object;
    }

    private String canonical(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("curve value is required");
        if (value.signum() == 0) return "0";
        return value.stripTrailingZeros().toPlainString();
    }
}
