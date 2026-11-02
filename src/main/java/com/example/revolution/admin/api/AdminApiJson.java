package com.example.revolution.admin.api;

import com.example.revolution.admin.definition.AdminMobSnapshot;
import com.example.revolution.admin.definition.AdminPlayerSnapshot;
import com.example.revolution.admin.definition.AdminRuneSnapshot;
import java.util.List;
import java.util.Map;

/** Minimal deterministic JSON writer for Admin API response DTOs. No request parsing happens here. */
final class AdminApiJson {
    private AdminApiJson() {}

    static String resources(List<String> resources) {
        StringBuilder out = new StringBuilder("{\"resources\":[");
        for (int i = 0; i < resources.size(); i++) {
            if (i > 0) out.append(',');
            quote(out, resources.get(i));
        }
        return out.append("]}").toString();
    }

    static String players(List<AdminPlayerSnapshot> players) {
        StringBuilder out = new StringBuilder("{\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) out.append(',');
            AdminPlayerSnapshot player = players.get(i);
            out.append('{');
            field(out, "uuid", player.uuid).append(',');
            field(out, "name", player.name).append(',');
            out.append("\"online\":").append(player.online).append(',');
            out.append("\"dimension\":").append(player.dimension).append(',');
            out.append("\"x\":").append(Double.toString(player.x)).append(',');
            out.append("\"y\":").append(Double.toString(player.y)).append(',');
            out.append("\"z\":").append(Double.toString(player.z)).append(',');
            out.append("\"runes\":{\"owned\":").append(player.runeOwned)
                    .append(",\"normal\":").append(player.runeNormal)
                    .append(",\"improved\":").append(player.runeImproved)
                    .append(",\"advanced\":").append(player.runeAdvanced).append("},");
            out.append("\"balances\":{");
            int balanceIndex = 0;
            for (Map.Entry<String, String> entry : player.balances.entrySet()) {
                if (balanceIndex++ > 0) out.append(',');
                quote(out, entry.getKey()).append(':');
                quote(out, entry.getValue());
            }
            out.append("}}");
        }
        return out.append("]}").toString();
    }

    static String runes(AdminRuneSnapshot snapshot) {
        StringBuilder out = new StringBuilder("{");
        out.append("\"schemaVersion\":").append(snapshot.schemaVersion).append(',');
        field(out, "playerUuid", snapshot.playerUuid).append(',');
        out.append("\"activeCollection\":").append(snapshot.activeCollection).append(',');
        out.append("\"collections\":[");
        for (int i = 0; i < snapshot.collections.size(); i++) {
            if (i > 0) out.append(',');
            AdminRuneSnapshot.Collection collection = snapshot.collections.get(i);
            out.append('{');
            out.append("\"index\":").append(collection.index).append(',');
            field(out, "name", collection.name).append(',');
            out.append("\"active\":").append(collection.active).append(',');
            out.append("\"runes\":[");
            for (int j = 0; j < collection.runes.size(); j++) {
                if (j > 0) out.append(',');
                AdminRuneSnapshot.Rune rune = collection.runes.get(j);
                out.append('{')
                        .append("\"slot\":").append(rune.slot).append(',')
                        .append("\"runeIndex\":").append(rune.runeIndex).append(',')
                        .append("\"layer\":").append(rune.layer).append(',')
                        .append("\"level\":").append(rune.level)
                        .append('}');
            }
            out.append("]}");
        }
        return out.append("]}").toString();
    }

    static String mobs(List<AdminMobSnapshot> mobs) {
        StringBuilder out = new StringBuilder("{\"mobs\":[");
        for (int i = 0; i < mobs.size(); i++) {
            if (i > 0) out.append(',');
            AdminMobSnapshot mob = mobs.get(i);
            out.append('{');
            field(out, "name", mob.name).append(',');
            field(out, "type", mob.type).append(',');
            field(out, "hp", mob.hp).append(',');
            field(out, "resource", mob.resource).append(',');
            field(out, "amount", mob.amount).append(',');
            out.append("\"x\":").append(Double.toString(mob.x)).append(',');
            out.append("\"y\":").append(Double.toString(mob.y)).append(',');
            out.append("\"z\":").append(Double.toString(mob.z)).append(',');
            out.append("\"enabled\":").append(mob.enabled).append('}');
        }
        return out.append("]}").toString();
    }

    static String ok() { return "{\"ok\":true}"; }

    private static StringBuilder field(StringBuilder out, String name, String value) {
        quote(out, name).append(':');
        return quote(out, value);
    }

    private static StringBuilder quote(StringBuilder out, String value) {
        out.append('"');
        String safe = value == null ? "" : value;
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
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
        return out.append('"');
    }
}
