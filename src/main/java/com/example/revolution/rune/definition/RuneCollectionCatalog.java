package com.example.revolution.rune.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical v2.3.0 content shape for all seven persistent rune collections.
 *
 * <p>This catalog owns stable collection/slot identity, technical names and effect targeting only.
 * Mutable gameplay numbers remain Balance Core data. Neutral numeric values here are schema
 * placeholders and must not be used as runtime balance.</p>
 */
public final class RuneCollectionCatalog {
    private static final Map<RuneCollectionId, RuneCollectionDefinition> COLLECTIONS = build();

    private RuneCollectionCatalog() {}

    public static RuneCollectionDefinition get(RuneCollectionId id) {
        return id == null ? null : COLLECTIONS.get(id);
    }

    public static List<RuneCollectionDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(COLLECTIONS.values()));
    }

    private static Map<RuneCollectionId, RuneCollectionDefinition> build() {
        EnumMap<RuneCollectionId, RuneCollectionDefinition> result = new EnumMap<>(RuneCollectionId.class);
        result.put(RuneCollectionId.COLLECTION_01, primaryCollection());

        RuneReferenceTheme[] themes = RuneReferenceTheme.values();
        RuneCollectionId[] ids = RuneCollectionId.values();
        for (int index = 1; index < ids.length; index++) {
            RuneReferenceTheme theme = themes[index];
            result.put(ids[index], technicalCollection(ids[index], theme));
        }
        return Collections.unmodifiableMap(result);
    }

    private static RuneCollectionDefinition primaryCollection() {
        List<RuneDefinition> runes = new ArrayList<>();
        for (int slot = 0; slot < PrimaryRuneCatalog.size(); slot++) {
            PrimaryRuneCatalog.Definition source = PrimaryRuneCatalog.at(slot);
            String suffix = twoDigits(slot + 1);
            runes.add(new RuneDefinition(
                    RuneCollectionId.COLLECTION_01,
                    RuneSlotId.fromLegacyIndex(slot),
                    "collection_01_slot_" + suffix,
                    source.name,
                    primaryEffects(source)));
        }
        return new RuneCollectionDefinition(RuneCollectionId.COLLECTION_01, "Collection 01", runes);
    }

    private static List<RuneEffectDefinition> primaryEffects(PrimaryRuneCatalog.Definition source) {
        List<RuneEffectDefinition> effects = new ArrayList<>();
        for (RuneStat stat : source.stats) {
            RuneDamageChannel damage = RuneDamageChannel.fromDamageStat(stat.type);
            if (damage != null) {
                effects.add(RuneEffectDefinition.damage(
                        stat.type.isDamageMultiplier() ? RuneEffectType.DAMAGE_MULTIPLIER : RuneEffectType.DAMAGE_FLAT,
                        damage,
                        neutralValue(stat.type)));
                continue;
            }
            RuneDamageChannel vulnerability = RuneDamageChannel.fromVulnerabilityStat(stat.type);
            if (vulnerability != null) {
                effects.add(RuneEffectDefinition.damage(RuneEffectType.VULNERABILITY, vulnerability, 0.0D));
                continue;
            }
            switch (stat.type) {
                case ALL_DAMAGE_VULNERABILITY:
                    effects.add(new RuneEffectDefinition(
                            RuneEffectType.VULNERABILITY, null, "ALL_DAMAGE", null, null,
                            0.0D, 0.0D, 0.0D));
                    break;
                case ALL_DAMAGE_MULTIPLIER:
                    effects.add(RuneEffectDefinition.scalar(RuneEffectType.ALL_DAMAGE_MULTIPLIER, 1.0D));
                    break;
                case COIN_REWARD_PERCENT:
                    effects.add(RuneEffectDefinition.scalar(RuneEffectType.MONSTER_GOLD_MULTIPLIER, 1.0D));
                    break;
                case RESOURCE_REWARD_PERCENT:
                    effects.add(RuneEffectDefinition.scalar(RuneEffectType.MONSTER_RESOURCE_MULTIPLIER, 1.0D));
                    break;
                case PET_XP_MULTIPLIER:
                    effects.add(RuneEffectDefinition.scalar(RuneEffectType.PET_XP_MULTIPLIER, 1.0D));
                    break;
                default:
                    break;
            }
        }
        for (RuneTargetedPower targeted : source.targetedPowers) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.TARGETED_POWER, null, targeted.targetKey, null, null,
                    0.0D, 0.0D, 0.0D));
        }
        for (RuneCompositeEffect composite : source.compositeEffects) {
            addCompositeShape(effects, composite);
        }
        if (effects.isEmpty()) {
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.ALL_DAMAGE_MULTIPLIER, 1.0D));
        }
        return effects;
    }

    private static void addCompositeShape(List<RuneEffectDefinition> effects, RuneCompositeEffect composite) {
        switch (composite.kind) {
            case CLICK_DAMAGE_PROC:
                effects.add(new RuneEffectDefinition(
                        RuneEffectType.PROC_CHANCE, RuneDamageChannel.CLICK, "CLICK", "HIT_MOB", null,
                        0.0D, 0.0D, 0.0D));
                effects.add(new RuneEffectDefinition(
                        RuneEffectType.PROC_DAMAGE_MULTIPLIER, RuneDamageChannel.CLICK, "CLICK", "HIT_MOB", null,
                        1.0D, 0.0D, 0.0D));
                break;
            case DELAYED_DAMAGE_MULTIPLIER:
                effects.add(new RuneEffectDefinition(
                        RuneEffectType.DELAYED_DAMAGE_BONUS,
                        RuneDamageChannel.fromDamageStat(composite.damageType),
                        null, "AFTER_DELAY", null,
                        1.0D, 0.0D, Math.max(0.0D, composite.primaryValue)));
                break;
            case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                effects.add(new RuneEffectDefinition(
                        RuneEffectType.TEMPORARY_DAMAGE_MULTIPLIER, null, "ALL_DAMAGE", "HIT_MOB", null,
                        1.0D, 0.0D, Math.max(0.0D, composite.durationSeconds)));
                break;
            case ENHANCEMENTS_MORE_EFFECTIVE:
                effects.add(RuneEffectDefinition.scalar(RuneEffectType.ENHANCEMENT_EFFECTIVENESS, 1.0D));
                break;
            default:
                break;
        }
    }

    private static RuneCollectionDefinition technicalCollection(
            RuneCollectionId id, RuneReferenceTheme theme) {
        RuneDamageChannel channel = theme.channel();
        List<RuneDefinition> runes = new ArrayList<>(RuneLayoutDefinition.SLOTS_PER_COLLECTION);
        for (int ordinal = 1; ordinal <= RuneLayoutDefinition.SLOTS_PER_COLLECTION; ordinal++) {
            String suffix = twoDigits(ordinal);
            String channelKey = channel.name().toLowerCase(java.util.Locale.ROOT);
            runes.add(new RuneDefinition(
                    id,
                    RuneSlotId.ofOrdinal(ordinal),
                    id.id() + "_slot_" + suffix,
                    id.id() + "_slot_" + suffix,
                    technicalEffects(channel, channelKey, ordinal)));
        }
        return new RuneCollectionDefinition(id, id.id(), runes);
    }

    private static List<RuneEffectDefinition> technicalEffects(
            RuneDamageChannel channel, String channelKey, int ordinal) {
        List<RuneEffectDefinition> effects = new ArrayList<>();
        if (ordinal <= 6) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_FLAT, channel, 0.0D));
        } else if (ordinal <= 10) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, channel, 1.0D));
        } else if (ordinal == 11) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.VULNERABILITY, channel, 0.0D));
        } else if (ordinal == 12) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.EFFECTIVENESS, channel, 1.0D));
        } else if (ordinal == 13) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.TARGETED_POWER, channel, channelKey + "_flat_01", null, null,
                    0.0D, 0.0D, 0.0D));
        } else if (ordinal == 14) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.PROC_CHANCE, channel, channel.name(), "HIT_MOB", null,
                    0.0D, 0.0D, 0.0D));
        } else if (ordinal == 15) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.PROC_DAMAGE_MULTIPLIER, channel, channel.name(), "HIT_MOB", null,
                    1.0D, 0.0D, 0.0D));
        } else if (ordinal == 16) {
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.ALL_DAMAGE_MULTIPLIER, 1.0D));
        } else if (ordinal == 17) {
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.MONSTER_GOLD_MULTIPLIER, 1.0D));
        } else if (ordinal == 18) {
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.MONSTER_RESOURCE_MULTIPLIER, 1.0D));
        } else if (ordinal == 19) {
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.PET_XP_MULTIPLIER, 1.0D));
        } else if (ordinal == 20) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.DELAYED_DAMAGE_BONUS, channel, channel.name(), "AFTER_DELAY", null,
                    1.0D, 0.0D, 1.0D));
        } else if (ordinal == 21) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.TEMPORARY_DAMAGE_MULTIPLIER, channel, channel.name(), "HIT_MOB", null,
                    1.0D, 0.0D, 1.0D));
        } else if (ordinal == 22) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.TEMPORARY_EFFECT_DURATION, channel, channel.name(), null, null,
                    1.0D, 0.0D, 0.0D));
        } else if (ordinal == 23) {
            effects.add(new RuneEffectDefinition(
                    RuneEffectType.ENHANCEMENT_EFFECTIVENESS, channel, channel.name(), null, null,
                    1.0D, 0.0D, 0.0D));
        } else if (ordinal == 24) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, channel, 1.0D));
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, RuneDamageChannel.CLICK, 1.0D));
        } else if (ordinal == 25) {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_FLAT, channel, 0.0D));
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.MONSTER_RESOURCE_MULTIPLIER, 1.0D));
        } else {
            effects.add(RuneEffectDefinition.damage(RuneEffectType.DAMAGE_MULTIPLIER, channel, 1.0D));
            effects.add(RuneEffectDefinition.scalar(RuneEffectType.ALL_DAMAGE_MULTIPLIER, 1.0D));
        }
        return effects;
    }

    private static double neutralValue(RuneStatType type) {
        return type != null && type.isDamageMultiplier() ? 1.0D : 0.0D;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
