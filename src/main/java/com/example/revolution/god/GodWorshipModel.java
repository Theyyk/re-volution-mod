package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.resource.definition.ResourceDefinitions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Stable v2.8.0 domain contract for the five independent God Worship tracks. */
public final class GodWorshipModel {
    private GodWorshipModel() {}

    public enum GodId { GOD_1, GOD_2, GOD_3, GOD_4, GOD_5 }
    public enum Phase { LOCKED, CHARGE, BUFF_ACTIVE, COOLDOWN }
    public enum ActivationMode { MANUAL, AUTO_WHEN_READY, LINKED_TO_GOD }
    public enum FeatureGate {
        GOD_AUTO_ACTIVATION,
        GOD_PERMANENT_BUFF_DURATION,
        GOD_SECONDARY_BUFFS,
        GOD_RESET_PRESERVATION
    }

    public static final class Definition {
        public final GodId id;
        public final String resource;
        public final EffectResolver.Stat primary;
        public final EffectResolver.Stat secondary;

        private Definition(GodId id, String resource, EffectResolver.Stat primary, EffectResolver.Stat secondary) {
            this.id = id;
            this.resource = resource;
            this.primary = primary;
            this.secondary = secondary;
        }

        public List<EffectResolver.Stat> channels(boolean secondaryEnabled) {
            if (!secondaryEnabled || secondary == null) return Collections.singletonList(primary);
            return Arrays.asList(primary, secondary);
        }
    }

    private static final Map<GodId, Definition> DEFINITIONS;
    static {
        EnumMap<GodId, Definition> values = new EnumMap<>(GodId.class);
        values.put(GodId.GOD_1, new Definition(GodId.GOD_1, ResourceDefinitions.RUNE_DUST,
                EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER));
        values.put(GodId.GOD_2, new Definition(GodId.GOD_2, ResourceDefinitions.RUNE_SHARDS,
                EffectResolver.Stat.DECAY_DAMAGE_MULTIPLIER, EffectResolver.Stat.BURNING_DAMAGE_MULTIPLIER));
        values.put(GodId.GOD_3, new Definition(GodId.GOD_3, "coins",
                EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, null));
        values.put(GodId.GOD_4, new Definition(GodId.GOD_4, "coins",
                EffectResolver.Stat.WITHER_DAMAGE_MULTIPLIER, EffectResolver.Stat.CURSE_DAMAGE_MULTIPLIER));
        values.put(GodId.GOD_5, new Definition(GodId.GOD_5, ResourceDefinitions.RUNE_CORES,
                EffectResolver.Stat.BLEEDING_DAMAGE_MULTIPLIER, EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER));
        DEFINITIONS = Collections.unmodifiableMap(values);
    }

    public static Definition definition(GodId id) {
        Definition definition = DEFINITIONS.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown god: " + id);
        return definition;
    }

    public static Map<GodId, Definition> definitions() { return DEFINITIONS; }

    public static final class State {
        public boolean unlocked;
        /** Unlocked gods start at level 1. Locked gods keep level 0. No upper cap is imposed. */
        public long level;
        public Phase phase = Phase.LOCKED;
        public long buffActiveUntilMillis;
        public long cooldownUntilMillis;
        public long chargeProgress;

        public State copy() {
            State copy = new State();
            copy.unlocked = unlocked;
            copy.level = level;
            copy.phase = phase;
            copy.buffActiveUntilMillis = buffActiveUntilMillis;
            copy.cooldownUntilMillis = cooldownUntilMillis;
            copy.chargeProgress = chargeProgress;
            return copy;
        }
    }

    public static final class Preference {
        public boolean autoWorshipEnabled;
        public ActivationMode activationMode = ActivationMode.MANUAL;
        public GodId linkedGodId;

        public void validateFor(GodId owner) {
            if (activationMode == ActivationMode.LINKED_TO_GOD && linkedGodId == null)
                throw new IllegalArgumentException("LINKED_TO_GOD requires linkedGodId");
            if (linkedGodId == owner) throw new IllegalArgumentException("A God cannot link to itself");
        }
    }

    public static final class PersistentState {
        public BigDecimal offerings = BigDecimal.ZERO;
        public long permanentDurationLevel;
        public final Map<GodId, Preference> preferences = new EnumMap<>(GodId.class);

        public PersistentState() {
            for (GodId id : GodId.values()) preferences.put(id, new Preference());
        }
    }

    public static final class RunState {
        public final Map<GodId, State> gods = new EnumMap<>(GodId.class);
        public RunState() { for (GodId id : GodId.values()) gods.put(id, new State()); }
    }
}
