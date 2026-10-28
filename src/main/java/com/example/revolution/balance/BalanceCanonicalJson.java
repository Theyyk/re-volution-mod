package com.example.revolution.balance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Deterministic semantic serialization used for revision hashing and diagnostics. */
final class BalanceCanonicalJson {
    private BalanceCanonicalJson() {}

    static String write(BalanceConfig config) {
        StringBuilder out = new StringBuilder();
        out.append('{');
        out.append("\"balanceSchemaVersion\":").append(config.getBalanceSchemaVersion()).append(',');
        out.append("\"curves\":{");

        List<String> ids = new ArrayList<>(config.getCurves().keySet());
        Collections.sort(ids);
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) out.append(',');
            String id = ids.get(i);
            appendString(out, id);
            out.append(':');
            appendCurve(out, config.getCurve(id));
        }
        out.append("}}");
        return out.toString();
    }

    private static void appendCurve(StringBuilder out, BalanceCurve curve) {
        out.append('{');
        out.append("\"mode\":");
        appendString(out, curve.getMode().name());
        switch (curve.getMode()) {
            case FIXED:
                out.append(",\"value\":");
                appendDecimal(out, curve.getFixedValue());
                break;
            case TABLE:
                out.append(",\"values\":{");
                int tableIndex = 0;
                for (Map.Entry<Integer, BigDecimal> entry : curve.getTable().entrySet()) {
                    if (tableIndex++ > 0) out.append(',');
                    appendString(out, Integer.toString(entry.getKey()));
                    out.append(':');
                    appendDecimal(out, entry.getValue());
                }
                out.append('}');
                break;
            case FORMULA:
                out.append(",\"base\":");
                appendDecimal(out, curve.getFormula().getBase());
                out.append(",\"growth\":");
                appendDecimal(out, curve.getFormula().getGrowth());
                break;
            case PIECEWISE:
                out.append(",\"segments\":[");
                for (int i = 0; i < curve.getSegments().size(); i++) {
                    if (i > 0) out.append(',');
                    BalanceCurve.Segment segment = curve.getSegments().get(i);
                    out.append("{\"from\":").append(segment.getFromInclusive());
                    out.append(",\"to\":").append(segment.getToInclusive());
                    out.append(",\"curve\":");
                    appendCurve(out, segment.getCurve());
                    out.append('}');
                }
                out.append(']');
                break;
            default:
                throw new IllegalArgumentException("unsupported curve mode " + curve.getMode());
        }
        out.append('}');
    }

    private static void appendDecimal(StringBuilder out, BigDecimal value) {
        appendString(out, canonicalDecimal(value));
    }

    private static String canonicalDecimal(BigDecimal value) {
        if (value == null || value.signum() == 0) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        out.append('"');
    }
}
