package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.definition.RuneStatType;

/**
 * Generic scalar effect accumulator shared by all rune collections.
 * Runtime systems may consume only the fields they currently support; unsupported reserved stats
 * remain visible in the aggregate without silently changing unrelated gameplay.
 */
public final class RuneEffectAggregate {
    public final RuneDamageMath.Snapshot damage = new RuneDamageMath.Snapshot();
    public double clickCritChancePercent;
    public double clickCritDamageMultiplier = 1.0D;
    public double coinRewardPercent;
    public double resourceRewardPercent;
    public double petXpMultiplier = 1.0D;
    public double legacyHeroXpMultiplier = 1.0D;
    public double magicSphereDurationSeconds;
    public double untargetedRunePowerFlat;

    public void apply(RuneStatType type, double value) {
        if (type == null || !Double.isFinite(value)) return;

        RuneDamageChannel damageChannel = RuneDamageChannel.fromDamageStat(type);
        if (damageChannel != null) {
            RuneDamageMath.ChannelStats stats = damage.channel(damageChannel);
            if (type.isDamageMultiplier()) stats.multiplier *= Math.max(0.0D, value);
            else stats.flat += Math.max(0.0D, value);
            return;
        }

        RuneDamageChannel vulnerabilityChannel = RuneDamageChannel.fromVulnerabilityStat(type);
        if (vulnerabilityChannel != null) {
            damage.channel(vulnerabilityChannel).vulnerabilityPercent += value;
            return;
        }

        switch (type) {
            case ALL_DAMAGE_VULNERABILITY:
                damage.allDamageVulnerabilityPercent += value;
                break;
            case ALL_DAMAGE_MULTIPLIER:
                damage.allDamageMultiplier *= Math.max(0.0D, value);
                break;
            case CLICK_CRIT_CHANCE:
                clickCritChancePercent += value;
                break;
            case CLICK_CRIT_DAMAGE_MULTIPLIER:
                clickCritDamageMultiplier *= Math.max(0.0D, value);
                break;
            case COIN_REWARD_PERCENT:
                coinRewardPercent += value;
                break;
            case RESOURCE_REWARD_PERCENT:
                resourceRewardPercent += value;
                break;
            case PET_XP_MULTIPLIER:
                petXpMultiplier *= Math.max(0.0D, value);
                break;
            case HERO_XP_MULTIPLIER:
                legacyHeroXpMultiplier *= Math.max(0.0D, value);
                break;
            case MAGIC_SPHERE_DURATION:
                magicSphereDurationSeconds += Math.max(0.0D, value);
                break;
            case RUNE_POWER_FLAT:
                untargetedRunePowerFlat += Math.max(0.0D, value);
                break;
            default:
                break;
        }
    }
}
