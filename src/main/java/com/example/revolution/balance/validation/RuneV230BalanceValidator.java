package com.example.revolution.balance.validation;

import com.example.revolution.balance.definition.BalanceConfig;
import com.example.revolution.rune.definition.RuneDamageChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Semantic guard for the complete v2.3 fixed-collection rune snapshot. */
final class RuneV230BalanceValidator {
    private static final int SLOT_COUNT = 26;
    private static final Set<String> EFFECT_TYPES = new HashSet<>(Arrays.asList(
            "STAT",
            "TARGETED_POWER",
            "CLICK_DAMAGE_PROC",
            "TEMPORARY_ALL_DAMAGE_MULTIPLIER",
            "EFFECTIVENESS",
            "DELAYED_DAMAGE_BONUS",
            "TEMPORARY_DAMAGE_MULTIPLIER",
            "TEMPORARY_EFFECT_DURATION",
            "ENHANCEMENT_EFFECTIVENESS"));
    private static final Set<String> CHANNEL_TARGET_TYPES = new HashSet<>(Arrays.asList(
            "EFFECTIVENESS",
            "DELAYED_DAMAGE_BONUS",
            "TEMPORARY_DAMAGE_MULTIPLIER",
            "TEMPORARY_EFFECT_DURATION",
            "ENHANCEMENT_EFFECTIVENESS"));

    private RuneV230BalanceValidator() {}

    static void validateIfPresent(BalanceConfig config, java.util.List<String> errors) {
        if (config == null || errors == null) return;
        if (config.getValue("runes.collection_02.effect_template") == null) return;

        for (int collection = 2; collection <= 7; collection++) {
            String namespace = String.format(Locale.ROOT, "collection_%02d", collection);
            validateCollection(config, namespace, errors);
        }
    }

    private static void validateCollection(BalanceConfig config, String namespace, java.util.List<String> errors) {
        String prefix = "runes." + namespace + ".";
        String template = required(config, prefix + "effect_template", errors);
        String primaryChannel = required(config, prefix + "primary_channel", errors);
        if (primaryChannel != null && channel(primaryChannel) == null) {
            errors.add(prefix + "primary_channel is unsupported: " + primaryChannel);
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            String group = resolved(config, namespace, template, "slot." + slot + ".group");
            if (blank(group)) {
                errors.add(prefix + "slot." + slot + ".group is required");
                continue;
            }
            validateGroup(config, namespace, template, group.trim(), errors);
        }

        String templatePrefix = template == null ? null : "runes.templates." + template + ".slot.";
        if (templatePrefix != null) {
            for (String key : config.getValues().keySet()) {
                if (!key.startsWith(templatePrefix) || !key.endsWith(".group")) continue;
                String raw = key.substring(templatePrefix.length(), key.length() - ".group".length());
                try {
                    int slot = Integer.parseInt(raw);
                    if (slot < 0 || slot >= SLOT_COUNT) errors.add(key + " is outside fixed slot range 0..25");
                } catch (NumberFormatException e) {
                    errors.add(key + " has invalid slot id");
                }
            }
        }

        for (int sourceRank = 1; sourceRank <= 3; sourceRank++) {
            String rankPrefix = prefix + "rankup.rank." + sourceRank + ".";
            integerEquals(config, rankPrefix + "target", sourceRank + 1, errors);
            integerEquals(config, rankPrefix + "required_slots", SLOT_COUNT, errors);
            String resource = required(config, rankPrefix + "resource", errors);
            if (resource != null && !("rune_shards".equals(resource) || "rune_cores".equals(resource))) {
                errors.add(rankPrefix + "resource is unsupported: " + resource);
            }
            if (config.getCurve(rankPrefix + "cost") == null) errors.add(rankPrefix + "cost curve is required");
        }
        if (config.getValue(prefix + "rankup.rank.4.target") != null) {
            errors.add(prefix + "rankup.rank.4.target must be absent because MYTHIC is maximum gameplay rank");
        }
        for (int rank = 1; rank <= 4; rank++) {
            if (config.getCurve(prefix + "purchase.rank." + rank) == null) {
                errors.add(prefix + "purchase.rank." + rank + " curve is required");
            }
        }
    }

    private static void validateGroup(
            BalanceConfig config,
            String namespace,
            String template,
            String group,
            java.util.List<String> errors) {
        String countRaw = resolved(config, namespace, template, "effect." + group + ".count");
        int count = positiveInt(countRaw, "runes." + namespace + ".effect." + group + ".count", errors);
        if (count <= 0) return;
        for (int effectIndex = 1; effectIndex <= count; effectIndex++) {
            String effectPrefix = "effect." + group + "." + effectIndex + ".";
            String type = resolved(config, namespace, template, effectPrefix + "type");
            if (blank(type) || !EFFECT_TYPES.contains(type.trim().toUpperCase(Locale.ROOT))) {
                errors.add("runes." + namespace + "." + effectPrefix + "type is unsupported: " + type);
                continue;
            }
            type = type.trim().toUpperCase(Locale.ROOT);
            String target = resolved(config, namespace, template, effectPrefix + "target_selector");
            if (blank(target)) {
                errors.add("runes." + namespace + "." + effectPrefix + "target_selector is required");
            } else if (CHANNEL_TARGET_TYPES.contains(type) && channel(target) == null) {
                errors.add("runes." + namespace + "." + effectPrefix + "target_selector must be a damage channel");
            }

            for (int rank = 1; rank <= 4; rank++) {
                String raw = resolved(config, namespace, template, effectPrefix + "rank." + rank + ".primary");
                nonNegativeFinite(raw, "runes." + namespace + "." + effectPrefix + "rank." + rank + ".primary", errors);
            }
            if ("DELAYED_DAMAGE_BONUS".equals(type)) {
                String raw = resolved(config, namespace, template, effectPrefix + "delay_seconds");
                positiveFinite(raw, "runes." + namespace + "." + effectPrefix + "delay_seconds", errors);
                String condition = resolved(config, namespace, template, effectPrefix + "condition");
                if (!blank(condition) && !"TARGET_ALIVE".equalsIgnoreCase(condition)) {
                    errors.add("runes." + namespace + "." + effectPrefix + "condition is unsupported: " + condition);
                }
            }
            if ("TEMPORARY_ALL_DAMAGE_MULTIPLIER".equals(type)
                    || "TEMPORARY_DAMAGE_MULTIPLIER".equals(type)) {
                for (int rank = 1; rank <= 4; rank++) {
                    String raw = resolved(config, namespace, template,
                            effectPrefix + "rank." + rank + ".duration_seconds");
                    if (blank(raw)) raw = resolved(config, namespace, template, effectPrefix + "duration_seconds");
                    positiveFinite(raw, "runes." + namespace + "." + effectPrefix
                            + "rank." + rank + ".duration_seconds", errors);
                }
            }
        }
    }

    private static String resolved(BalanceConfig config, String namespace, String template, String suffix) {
        String direct = config.getValue("runes." + namespace + "." + suffix);
        String value = direct;
        if (value == null && !blank(template)) value = config.getValue("runes.templates." + template.trim() + "." + suffix);
        if (value == null) return null;
        String channel = config.getValue("runes." + namespace + ".primary_channel");
        if (channel == null) return value;
        return value.replace("{CHANNEL}", channel.trim().toUpperCase(Locale.ROOT))
                .replace("{channel}", channel.trim().toLowerCase(Locale.ROOT));
    }

    private static RuneDamageChannel channel(String raw) {
        if (blank(raw)) return null;
        try { return RuneDamageChannel.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static String required(BalanceConfig config, String key, java.util.List<String> errors) {
        String value = config.getValue(key);
        if (blank(value)) {
            errors.add(key + " is required");
            return null;
        }
        return value.trim();
    }

    private static int positiveInt(String raw, String key, java.util.List<String> errors) {
        if (blank(raw)) { errors.add(key + " is required"); return -1; }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) errors.add(key + " must be > 0");
            return value;
        } catch (NumberFormatException e) {
            errors.add(key + " must be an integer");
            return -1;
        }
    }

    private static void integerEquals(BalanceConfig config, String key, int expected, java.util.List<String> errors) {
        String raw = config.getValue(key);
        if (blank(raw)) { errors.add(key + " is required"); return; }
        try {
            if (Integer.parseInt(raw.trim()) != expected) errors.add(key + " must equal " + expected);
        } catch (NumberFormatException e) {
            errors.add(key + " must be an integer");
        }
    }

    private static void nonNegativeFinite(String raw, String key, java.util.List<String> errors) {
        Double value = finite(raw, key, errors);
        if (value != null && value < 0.0D) errors.add(key + " must be >= 0");
    }

    private static void positiveFinite(String raw, String key, java.util.List<String> errors) {
        Double value = finite(raw, key, errors);
        if (value != null && value <= 0.0D) errors.add(key + " must be > 0");
    }

    private static Double finite(String raw, String key, java.util.List<String> errors) {
        if (blank(raw)) { errors.add(key + " is required"); return null; }
        try {
            double value = Double.parseDouble(raw.trim());
            if (!Double.isFinite(value)) { errors.add(key + " must be finite"); return null; }
            return value;
        } catch (NumberFormatException e) {
            errors.add(key + " must be numeric");
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
