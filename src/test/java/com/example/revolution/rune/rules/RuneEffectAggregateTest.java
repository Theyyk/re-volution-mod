package com.example.revolution.rune.rules;

import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.definition.RuneStatType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RuneEffectAggregateTest {
    @Test
    void damageStatsRouteToCanonicalChannels() {
        RuneEffectAggregate aggregate = new RuneEffectAggregate();
        aggregate.apply(RuneStatType.BURNING_DAMAGE_FLAT, 12.0D);
        aggregate.apply(RuneStatType.BURNING_DAMAGE_MULTIPLIER, 1.5D);
        aggregate.apply(RuneStatType.BURNING_VULNERABILITY, 20.0D);
        aggregate.apply(RuneStatType.ALL_DAMAGE_VULNERABILITY, 10.0D);
        aggregate.apply(RuneStatType.ALL_DAMAGE_MULTIPLIER, 2.0D);

        RuneDamageMath.ChannelStats burning = aggregate.damage.existing(RuneDamageChannel.BURNING);
        assertEquals(12.0D, burning.flat, 0.000001D);
        assertEquals(1.5D, burning.multiplier, 0.000001D);
        assertEquals(20.0D, burning.vulnerabilityPercent, 0.000001D);
        assertEquals(10.0D, aggregate.damage.allDamageVulnerabilityPercent, 0.000001D);
        assertEquals(2.0D, aggregate.damage.allDamageMultiplier, 0.000001D);
    }

    @Test
    void multipliersStackMultiplicativelyAndFlatStatsAdd() {
        RuneEffectAggregate aggregate = new RuneEffectAggregate();
        aggregate.apply(RuneStatType.DECAY_DAMAGE_FLAT, 5.0D);
        aggregate.apply(RuneStatType.DECAY_DAMAGE_FLAT, 7.0D);
        aggregate.apply(RuneStatType.DECAY_DAMAGE_MULTIPLIER, 2.0D);
        aggregate.apply(RuneStatType.DECAY_DAMAGE_MULTIPLIER, 1.5D);
        aggregate.apply(RuneStatType.PET_XP_MULTIPLIER, 1.2D);
        aggregate.apply(RuneStatType.PET_XP_MULTIPLIER, 1.5D);

        RuneDamageMath.ChannelStats decay = aggregate.damage.existing(RuneDamageChannel.DECAY);
        assertEquals(12.0D, decay.flat, 0.000001D);
        assertEquals(3.0D, decay.multiplier, 0.000001D);
        assertEquals(1.8D, aggregate.petXpMultiplier, 0.000001D);
    }

    @Test
    void rewardAndReservedVisibleStatsRemainIndependent() {
        RuneEffectAggregate aggregate = new RuneEffectAggregate();
        aggregate.apply(RuneStatType.COIN_REWARD_PERCENT, 15.0D);
        aggregate.apply(RuneStatType.RESOURCE_REWARD_PERCENT, 25.0D);
        aggregate.apply(RuneStatType.MAGIC_SPHERE_DURATION, 4.0D);

        assertEquals(15.0D, aggregate.coinRewardPercent, 0.000001D);
        assertEquals(25.0D, aggregate.resourceRewardPercent, 0.000001D);
        assertEquals(4.0D, aggregate.magicSphereDurationSeconds, 0.000001D);
    }
}
