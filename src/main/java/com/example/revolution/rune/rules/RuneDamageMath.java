package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneDamageChannel;
import java.util.EnumMap;
import java.util.Map;

/** Pure shared damage math for every canonical rune damage/status channel. */
public final class RuneDamageMath {
    public static final class ChannelStats {
        public double flat;
        public double multiplier = 1.0D;
        public double effectivenessMultiplier = 1.0D;
        public double vulnerabilityPercent;

        public ChannelStats copy() {
            ChannelStats copy = new ChannelStats();
            copy.flat = flat;
            copy.multiplier = multiplier;
            copy.effectivenessMultiplier = effectivenessMultiplier;
            copy.vulnerabilityPercent = vulnerabilityPercent;
            return copy;
        }
    }

    public static final class Snapshot {
        private final EnumMap<RuneDamageChannel, ChannelStats> channels =
                new EnumMap<>(RuneDamageChannel.class);
        public double allDamageVulnerabilityPercent;
        public double allDamageMultiplier = 1.0D;
        public boolean temporaryAllDamageActive;
        public double temporaryAllDamageMultiplier = 1.0D;

        public ChannelStats channel(RuneDamageChannel channel) {
            if (channel == null) throw new IllegalArgumentException("damage channel is required");
            ChannelStats stats = channels.get(channel);
            if (stats == null) {
                stats = new ChannelStats();
                channels.put(channel, stats);
            }
            return stats;
        }

        public ChannelStats existing(RuneDamageChannel channel) {
            return channel == null ? null : channels.get(channel);
        }

        public Map<RuneDamageChannel, ChannelStats> channels() {
            EnumMap<RuneDamageChannel, ChannelStats> copy = new EnumMap<>(RuneDamageChannel.class);
            for (Map.Entry<RuneDamageChannel, ChannelStats> entry : channels.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
            return java.util.Collections.unmodifiableMap(copy);
        }
    }

    private RuneDamageMath() {}

    public static double damage(double baseDamage, RuneDamageChannel channel, Snapshot snapshot) {
        if (snapshot == null || channel == null) return sanitize(baseDamage);
        ChannelStats stats = snapshot.existing(channel);
        double flat = stats == null ? 0.0D : nonNegative(stats.flat);
        double multiplier = stats == null ? 1.0D : safeMultiplier(stats.multiplier);
        double effectiveness = stats == null ? 1.0D : safeMultiplier(stats.effectivenessMultiplier);
        double vulnerability = stats == null ? 0.0D : stats.vulnerabilityPercent;

        double damage = Math.max(0.0D, sanitize(baseDamage) + flat);
        damage *= multiplier;
        damage *= effectiveness;
        damage *= vulnerabilityMultiplier(vulnerability + snapshot.allDamageVulnerabilityPercent);
        damage *= safeMultiplier(snapshot.allDamageMultiplier);
        if (snapshot.temporaryAllDamageActive) {
            damage *= safeMultiplier(snapshot.temporaryAllDamageMultiplier);
        }
        return sanitize(damage);
    }

    public static double statusDamage(RuneDamageChannel channel, Snapshot snapshot) {
        return damage(0.0D, channel, snapshot);
    }

    public static double vulnerabilityMultiplier(double percent) {
        if (!Double.isFinite(percent)) return 1.0D;
        return Math.max(0.0D, 1.0D + percent / 100.0D);
    }

    public static double safeMultiplier(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 1.0D;
    }

    public static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public static double sanitize(double value) {
        if (!Double.isFinite(value)) return Float.MAX_VALUE;
        return Math.max(0.0D, Math.min((double) Float.MAX_VALUE, value));
    }
}
