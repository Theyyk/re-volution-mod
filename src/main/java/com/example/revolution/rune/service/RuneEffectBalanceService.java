package com.example.revolution.rune.service;

import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.rune.config.RuneStatusConfig;
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

    public String group(int statusId, int slot) {
        if (slot < 0) throw new IllegalArgumentException("slot must be >= 0");
        return required(statusId, "slot." + slot + ".group");
    }

    public List<ResolvedEffect> effects(int statusId, int slot, int rank, int enhancementLevel) {
        if (rank <= 0) return Collections.emptyList();
        String group = group(statusId, slot);
        int count = positiveInt(required(statusId, "effect." + group + ".count"), "effect count");
        List<ResolvedEffect> result = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            String prefix = "effect." + group + "." + index + ".";
            EffectType type = enumValue(EffectType.class, required(statusId, prefix + "type"), "effect type");
            String tags = required(statusId, prefix + "tags");
            String target = required(statusId, prefix + "target_selector");
            String statTypeRaw = optional(statusId, prefix + "stat_type");
            RuneStatType statType = statTypeRaw == null ? null
                    : enumValue(RuneStatType.class, statTypeRaw, "stat type");
            boolean acceptsTargetedPower = booleanValue(optional(statusId, prefix + "accepts_targeted_power"), false);
            double enhancementStep = nonNegative(optionalDouble(statusId, prefix + "enhancement_step", 0.0D), "enhancement step");
            String enhancementField = optional(statusId, prefix + "enhancement_field");
            double enhancementMultiplier = 1.0D + Math.max(0, enhancementLevel) * enhancementStep;

            double rawPrimary = rankDouble(statusId, prefix, rank, "primary", 0.0D);
            double rawSecondary = rankDouble(statusId, prefix, rank, "secondary", 0.0D);
            double primary = rawPrimary;
            double secondary = rawSecondary;
            if ("PRIMARY".equalsIgnoreCase(enhancementField)) primary *= enhancementMultiplier;
            else if ("SECONDARY".equalsIgnoreCase(enhancementField)) secondary *= enhancementMultiplier;
            else if (enhancementField != null && !"NONE".equalsIgnoreCase(enhancementField)) {
                throw new IllegalStateException("unsupported enhancement field: " + enhancementField);
            }

            double targetedPowerScale = 1.0D;
            if (acceptsTargetedPower) {
                double normalPrimary = rankDouble(statusId, prefix, 1, "primary", 0.0D);
                if (normalPrimary <= 0.0D) {
                    throw new IllegalStateException("targeted-power effect requires positive rank.1.primary for " + group);
                }
                targetedPowerScale = rawPrimary / normalPrimary;
                if ("PRIMARY".equalsIgnoreCase(enhancementField)) targetedPowerScale *= enhancementMultiplier;
            }

            double baseDuration = nonNegative(optionalDouble(statusId, prefix + "duration_seconds", 0.0D), "duration");
            double duration = nonNegative(
                    optionalDouble(statusId, prefix + "rank." + rank + ".duration_seconds", baseDuration),
                    "duration");
            int maxStacks = nonNegativeInt(optional(statusId, prefix + "max_stacks"), 0, "max stacks");
            result.add(new ResolvedEffect(group, index, type, tags, target, statType,
                    acceptsTargetedPower, targetedPowerScale, primary, secondary, duration, maxStacks));
        }
        return Collections.unmodifiableList(result);
    }

    private double rankDouble(int statusId, String prefix, int rank, String field, double fallback) {
        return nonNegative(optionalDouble(statusId, prefix + "rank." + rank + "." + field, fallback), field);
    }

    private String required(int statusId, String suffix) {
        String value = optional(statusId, suffix);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("missing rune effect balance value: " + key(statusId, suffix));
        }
        return value.trim();
    }

    private String optional(int statusId, String suffix) {
        if (!RuneStatusConfig.isRuneStatus(statusId)) throw new IllegalArgumentException("status id must be positive");
        String value = balance.getActiveSnapshot().getConfig().getValue(key(statusId, suffix));
        return value == null ? null : value.trim();
    }

    private String key(int statusId, String suffix) {
        return "runes." + RuneStatusConfig.statusKey(statusId) + "." + suffix;
    }

    private double optionalDouble(int statusId, String suffix, double fallback) {
        String raw = optional(statusId, suffix);
        if (raw == null) return fallback;
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) throw new NumberFormatException("non-finite");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid numeric rune effect value " + key(statusId, suffix) + "=" + raw, e);
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
