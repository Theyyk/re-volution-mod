package com.example.revolution.achievement;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compact Balance Core codec for the ZBT Achievement content overlay. */
public final class BalanceAchievementCatalogFactory {
    private BalanceAchievementCatalogFactory() {}

    public static AchievementCatalog fromBalance(BalanceService balance) {
        if (balance == null || balance.getActiveSnapshot() == null) throw new IllegalArgumentException("active balance is required");
        Map<String, String> values = balance.getActiveSnapshot().getConfig().getValues();
        String rawIds = required(values, "achievement.ids");
        List<AchievementDefinition> definitions = new ArrayList<>();
        for (String rawId : rawIds.split(",")) {
            String id = rawId.trim();
            if (id.isEmpty()) continue;
            String prefix = "achievement." + id + ".";
            String name = required(values, prefix + "name");
            AchievementCategory category = AchievementCategory.valueOf(required(values, prefix + "category").trim().toUpperCase(java.util.Locale.ROOT));
            GameplayEventType eventType = GameplayEventType.valueOf(required(values, prefix + "event").trim().toUpperCase(java.util.Locale.ROOT));
            boolean hidden = Boolean.parseBoolean(values.get(prefix + "hidden"));
            Map<String, String> filters = filters(values.get(prefix + "filters"));
            List<AchievementStage> stages = stages(required(values, prefix + "stages"));
            definitions.add(new AchievementDefinition(id, name, category, eventType, filters, hidden, stages));
        }
        if (definitions.isEmpty()) throw new IllegalStateException("Achievement catalog cannot be empty");
        return new AchievementCatalog(definitions);
    }

    private static List<AchievementStage> stages(String encoded) {
        List<AchievementStage> result = new ArrayList<>();
        String[] entries = encoded.split(";");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i].trim();
            if (entry.isEmpty()) continue;
            String[] fields = entry.split("\\|");
            if (fields.length != 5) throw new IllegalArgumentException("Achievement stage must be target|crystals|offerings|auraKeys|points: " + entry);
            result.add(new AchievementStage(result.size() + 1,
                    decimal(fields[0]), decimal(fields[1]), decimal(fields[2]), decimal(fields[3]), longValue(fields[4])));
        }
        return result;
    }

    private static Map<String, String> filters(String encoded) {
        Map<String, String> result = new LinkedHashMap<>();
        if (encoded == null || encoded.trim().isEmpty()) return result;
        for (String entry : encoded.split(",")) {
            int split = entry.indexOf('=');
            if (split <= 0 || split == entry.length() - 1) throw new IllegalArgumentException("Invalid Achievement filter: " + entry);
            result.put(entry.substring(0, split).trim(), entry.substring(split + 1).trim());
        }
        return result;
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing balance value '" + key + "'");
        return value;
    }
    private static BigDecimal decimal(String value) {
        BigDecimal parsed = new BigDecimal(value.trim());
        if (parsed.signum() < 0) throw new IllegalArgumentException("negative Achievement balance value");
        return parsed;
    }
    private static long longValue(String value) {
        long parsed = Long.parseLong(value.trim());
        if (parsed < 0L) throw new IllegalArgumentException("negative Achievement points reward");
        return parsed;
    }
}
