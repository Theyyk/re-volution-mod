package com.example.revolution.rune.definition;

/**
 * Data-only generic rune effect definition.
 *
 * <p>Target is intentionally a stable selector string rather than a Java handler name. Damage
 * effects normally use a RuneDamageChannel; family/effect selectors may use targetSelector.
 * Runtime systems decide which fields are meaningful for each RuneEffectType and Balance Core
 * validation rejects invalid combinations before activation.</p>
 */
public final class RuneEffectDefinition {
    public final RuneEffectType type;
    public final RuneDamageChannel damageChannel;
    public final String targetSelector;
    public final String trigger;
    public final String condition;
    public final double value;
    public final double secondaryValue;
    public final double durationSeconds;

    public RuneEffectDefinition(
            RuneEffectType type,
            RuneDamageChannel damageChannel,
            String targetSelector,
            String trigger,
            String condition,
            double value,
            double secondaryValue,
            double durationSeconds) {
        if (type == null) throw new IllegalArgumentException("rune effect type is required");
        if (!Double.isFinite(value) || !Double.isFinite(secondaryValue) || !Double.isFinite(durationSeconds)) {
            throw new IllegalArgumentException("rune effect numeric values must be finite");
        }
        if (durationSeconds < 0.0D) throw new IllegalArgumentException("rune effect duration cannot be negative");
        this.type = type;
        this.damageChannel = damageChannel;
        this.targetSelector = clean(targetSelector);
        this.trigger = clean(trigger);
        this.condition = clean(condition);
        this.value = value;
        this.secondaryValue = secondaryValue;
        this.durationSeconds = durationSeconds;
    }

    public static RuneEffectDefinition damage(RuneEffectType type, RuneDamageChannel channel, double value) {
        if (type != RuneEffectType.DAMAGE_FLAT
                && type != RuneEffectType.DAMAGE_MULTIPLIER
                && type != RuneEffectType.VULNERABILITY
                && type != RuneEffectType.EFFECTIVENESS) {
            throw new IllegalArgumentException("effect type does not use a direct damage channel target: " + type);
        }
        if (channel == null) throw new IllegalArgumentException("damage channel is required");
        return new RuneEffectDefinition(type, channel, null, null, null, value, 0.0D, 0.0D);
    }

    public static RuneEffectDefinition scalar(RuneEffectType type, double value) {
        return new RuneEffectDefinition(type, null, null, null, null, value, 0.0D, 0.0D);
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
