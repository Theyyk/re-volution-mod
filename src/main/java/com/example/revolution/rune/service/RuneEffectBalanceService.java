package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneStatType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Resolves canonical rune-effect definitions from the active Balance Core snapshot. */
public final class RuneEffectBalanceService {
    public enum EffectType {
        STAT,
        TARGETED_POWER,
        CLICK_DAMAGE_PROC,
        TEMPORARY_ALL_DAMAGE_MULTIPLIER
    }

    public static final class ResolvedEffect {
        public final String group;
        public final int index;
        public final EffectType type;
        public final String tags;
        public final String targetSelector;
        public final RuneStatType statType;
        public final boolean acceptsTargetedPower;
        public final double targetedPowerScale;
        public final double primaryValue;
        public final double secondaryValue;
        public final double durationSeconds;
        public final int maxStacks;

        private ResolvedEffect(
                String group,
                int index,
                EffectType type,
                String tags,
                String targetSelector,
                RuneStatType statType,
                boolean acceptsTargetedPower,
                double targetedPowerScale,
                double primaryValue,
                double secondaryValue,
                double durationSeconds,
                int maxStacks) {
            this.group = group;
            this.index = index;
            this.type = type;
            this.tags = tags;
            this.targetSelector = targetSelector;
            this.statType = statType;
            this.acceptsTargetedPower = acceptsTargetedPower;
            this.targetedPowerScale = targetedPowerScale;
            this.primaryValue = primaryValue;
            this.secondaryValue = secondaryValue;
            this.durationSeconds = durationSeconds;
            this.maxStacks = maxStacks;
        }
    }

    private final BalanceService balance;

    public RuneEffectBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance service is required");
        this.balance = balance;
    }

    /** Legacy C1/status facade retained at the caller boundary. */
    public String group(int statusId, int slot) {
        return group(legacyNamespace(statusId), slot);
    }

    /** Stable v2.3.0 collection identity, independent from GUI position and legacy status ids. */
    public String group(RuneCollectionId collectionId, int slot) {
        return group(collectionNamespace(collectionId), slot);
    }

    private String group(String namespace, int slot) {
        if (slot < 0) throw new IllegalArgumentException("slot must be >= 0");
        return required(namespace, "slot." + slot + ".group");
    }

    public List<ResolvedEffect> effects(int statusId, int slot, int rank, int enhancementLevel) {
        return effects(legacyNamespace(statusId), slot, rank, enhancementLevel);
    }

    public List<ResolvedEffect> effects(
            RuneCollectionId collectionId, int slot, int rank, int enhancementLevel) {
        return effects(collectionNamespace(collectionId), slot, rank, enhancementLevel);
    }

    private List<ResolvedEffect> effects(
            String namespace, int slot, int rank, int enhancementLevel) {
        if (rank <= 0) return Collections.emptyList();
        String group = group(namespace, slot);
        int count = positiveInt(required(namespace, "effect." + group + ".count"), "effect count");
        List<ResolvedEffect> result = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            String prefix = "effect." + group + "." + index + ".";
            EffectType type = enumValue(EffectType.class, required(namespace, prefix + "type"), "effect type");
            String tags = required(namespace, prefix + "tags");
            String target = required(namespace, prefix + "target_selector");
            String statTypeRaw = optional(namespace, prefix + "stat_type");
            RuneStatType statType = statTypeRaw == null ? null
                    : enumValue(RuneStatType.class, statTypeRaw, "stat type");
            boolean acceptsTargetedPower = booleanValue(optional(namespace, prefix + "accepts_targeted_power"), false);
            double enhancementStep = nonNegative(optionalDouble(namespace, prefix + "enhancement_step", 0.0D), "enhancement step");
            String enhancementField = optional(namespace, prefix + "enhancement_field");
            double enhancementMultiplier = 1.0D + Math.max(0, enhancementLevel) * enhancementStep;

            double rawPrimary = rankDouble(namespace, prefix, rank, "primary", 0.0D);
            double rawSecondary = rankDouble(namespace, prefix, rank, "secondary", 0.0D);
            double primary = rawPrimary;
            double secondary = rawSecondary;
            if ("PRIMARY".equalsIgnoreCase(enhancementField)) primary *= enhancementMultiplier;
            else if ("SECONDARY".equalsIgnoreCase(enhancementField)) secondary *= enhancementMultiplier;
            else if (enhancementField != null && !"NONE".equalsIgnoreCase(enhancementField)) {
                throw new IllegalStateException("unsupported enhancement field: " + enhancementField);
            }

            // TARGETED_POWER already carries its own rank/enhancement value. It is summed by target group
            // and added once to the target flat value before shared channel multipliers.
            double targetedPowerScale = 1.0D;

            double baseDuration = nonNegative(optionalDouble(namespace, prefix + "duration_seconds", 0.0D), "duration");
            double duration = nonNegative(
                    optionalDouble(namespace, prefix + "rank." + rank + ".duration_seconds", baseDuration),
                    "duration");
            int maxStacks = nonNegativeInt(optional(namespace, prefix + "max_stacks"), 0, "max stacks");
            result.add(new ResolvedEffect(group, index, type, tags, target, statType,
                    acceptsTargetedPower, targetedPowerScale, primary, secondary, duration, maxStacks));
        }
        return Collections.unmodifiableList(result);
    }

    private double rankDouble(String namespace, String prefix, int rank, String field, double fallback) {
        return nonNegative(optionalDouble(namespace, prefix + "rank." + rank + "." + field, fallback), field);
    }

    private String required(String namespace, String suffix) {
        String value = optional(namespace, suffix);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("missing rune effect balance value: " + key(namespace, suffix));
        }
        return value.trim();
    }

    /**
     * Direct collection values always win. Fixed v2.3 collections may opt into a shared technical
     * effect template through runes.<collection>.effect_template. This keeps draft numbers in Balance
     * Core without copying the same 26-slot skeleton six times.
     */
    private String optional(String namespace, String suffix) {
        String value = rawOptional(key(namespace, suffix));
        if (value == null) {
            String template = rawOptional(key(namespace, "effect_template"));
            if (template != null && !template.trim().isEmpty()) {
                value = rawOptional("runes.templates." + template.trim() + "." + suffix);
            }
        }
        return value == null ? null : substitute(namespace, value.trim());
    }

    private String rawOptional(String key) {
        return balance.getActiveSnapshot().getConfig().getValue(key);
    }

    private String substitute(String namespace, String value) {
        if (value == null || value.indexOf('{') < 0) return value;
        String channel = rawOptional(key(namespace, "primary_channel"));
        if (channel == null || channel.trim().isEmpty()) return value;
        String normalized = channel.trim().toUpperCase(Locale.ROOT);
        return value.replace("{CHANNEL}", normalized)
                .replace("{channel}", normalized.toLowerCase(Locale.ROOT));
    }

    private String key(String namespace, String suffix) {
        return "runes." + namespace + "." + suffix;
    }

    private String legacyNamespace(int statusId) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        return RuneStatusConfig.statusKey(statusId);
    }

    private String collectionNamespace(RuneCollectionId collectionId) {
        if (collectionId == null) throw new IllegalArgumentException("collection id is required");
        // Collection 1 keeps the existing production Balance Core namespace for backward compatibility.
        return collectionId == RuneCollectionId.COLLECTION_01 ? "poison" : collectionId.id();
    }

    private double optionalDouble(String namespace, String suffix, double fallback) {
        String raw = optional(namespace, suffix);
        if (raw == null) return fallback;
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) throw new NumberFormatException("non-finite");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid numeric rune effect value " + key(namespace, suffix) + "=" + raw, e);
        }
    }

    private int positiveInt(String raw, String label) {
        int value = integer(raw, label);
        if (value <= 0) throw new IllegalStateException(label + " must be > 0");
        return value;
    }

    private int nonNegativeInt(String raw, int fallback, String label) {
        if (raw == null) return fallback;
        int value = integer(raw, label);
        if (value < 0) throw new IllegalStateException(label + " must be >= 0");
        return value;
    }

    private int integer(String raw, String label) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { throw new IllegalStateException("invalid " + label + ": " + raw, e); }
    }

    private double nonNegative(double value, String label) {
        if (value < 0.0D) throw new IllegalStateException(label + " must be >= 0");
        return value;
    }

    private boolean booleanValue(String raw, boolean fallback) {
        if (raw == null) return fallback;
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        throw new IllegalStateException("invalid boolean rune effect value: " + raw);
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String raw, String label) {
        try { return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("unsupported " + label + ": " + raw, e); }
    }
}
