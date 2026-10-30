package com.example.revolution.rune.definition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Structural v2.3 catalog for technical collections; balance values remain in Balance Core. */
public final class RuneTechnicalCatalog {
    public static final class EffectLabel {
        public final String type;
        public final RuneDamageChannel channel;
        public final boolean runtimeNoOp;

        private EffectLabel(String type, RuneDamageChannel channel, boolean runtimeNoOp) {
            this.type = type;
            this.channel = channel;
            this.runtimeNoOp = runtimeNoOp;
        }
    }

    private RuneTechnicalCatalog() {}

    public static RuneDamageChannel primaryChannel(RuneCollectionId collectionId) {
        if (collectionId == null) return null;
        switch (collectionId) {
            case COLLECTION_02: return RuneDamageChannel.POISON;
            case COLLECTION_03: return RuneDamageChannel.DECAY;
            case COLLECTION_04: return RuneDamageChannel.WITHER;
            case COLLECTION_05: return RuneDamageChannel.BLEEDING;
            case COLLECTION_06: return RuneDamageChannel.BURNING;
            case COLLECTION_07: return RuneDamageChannel.CURSE;
            default: return null;
        }
    }

    public static List<EffectLabel> effects(RuneCollectionId collectionId, int runeIndex) {
        RuneDamageChannel channel = primaryChannel(collectionId);
        if (channel == null || runeIndex < 0 || runeIndex >= 26) return Collections.emptyList();
        switch (runeIndex) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6:
                return one("DAMAGE_FLAT", channel);
            case 7: case 8: case 9:
                return one("DAMAGE_MULTIPLIER", channel);
            case 10:
                return one("VULNERABILITY", channel);
            case 11:
                return one("ALL_DAMAGE_MULTIPLIER", null);
            case 12:
                return one("TARGETED_POWER", channel);
            case 13:
                return Arrays.asList(label("PROC_CHANCE", channel, false), label("PROC_DAMAGE_MULTIPLIER", channel, false));
            case 14:
                return one("TEMPORARY_DAMAGE_MULTIPLIER", channel);
            case 15:
                return one("ALL_DAMAGE_MULTIPLIER", null);
            case 16:
                return one("MONSTER_GOLD_MULTIPLIER", null);
            case 17:
                return one("MONSTER_RESOURCE_MULTIPLIER", null);
            case 18:
                return Collections.singletonList(label("PET_XP_MULTIPLIER", null, true));
            case 19:
                return one("DELAYED_DAMAGE_BONUS", channel);
            case 20:
                return one("EFFECTIVENESS", channel);
            case 21:
                return one("TEMPORARY_EFFECT_DURATION", channel);
            case 22:
                return one("ENHANCEMENT_EFFECTIVENESS", channel);
            case 23:
                return Arrays.asList(label("DAMAGE_MULTIPLIER", channel, false), label("DAMAGE_MULTIPLIER", RuneDamageChannel.CLICK, false));
            case 24:
                return Arrays.asList(label("DAMAGE_FLAT", channel, false), label("MONSTER_RESOURCE_MULTIPLIER", null, false));
            case 25:
                return Arrays.asList(label("DAMAGE_MULTIPLIER", channel, false), label("ALL_DAMAGE_MULTIPLIER", null, false));
            default:
                return Collections.emptyList();
        }
    }

    private static List<EffectLabel> one(String type, RuneDamageChannel channel) {
        return Collections.singletonList(label(type, channel, false));
    }

    private static EffectLabel label(String type, RuneDamageChannel channel, boolean runtimeNoOp) {
        return new EffectLabel(type, channel, runtimeNoOp);
    }
}
