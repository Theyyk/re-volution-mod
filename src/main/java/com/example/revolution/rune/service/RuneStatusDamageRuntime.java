package com.example.revolution.rune.service;

import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.rune.definition.RuneDamageChannel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;

/**
 * Generic server-authoritative damage application boundary for rune damage/status channels.
 *
 * <p>The runtime owns no balance, proc or duration rules. Callers resolve an already-calculated
 * amount and this class applies that amount through Minecraft's normal damage event pipeline while
 * attaching stable gameplay metadata. Managed mobs therefore report the actual accepted damage
 * through the shared GameplayEventSink instead of each status talking to Prestige directly.</p>
 */
public final class RuneStatusDamageRuntime {
    private RuneStatusDamageRuntime() {}

    public static boolean apply(
            EntityPlayerMP player,
            EntityLivingBase target,
            RuneDamageChannel channel,
            double amount) {
        if (player == null || target == null || channel == null) return false;
        if (player.world == null || target.world == null || player.world != target.world) return false;
        if (player.world.isRemote || target.world.isRemote || target.isDead) return false;
        if (!Double.isFinite(amount) || amount <= 0.0D) return false;

        float nativeAmount = (float) Math.min(amount, Float.MAX_VALUE);
        if (!Float.isFinite(nativeAmount) || nativeAmount <= 0.0F) return false;

        GameplayDamageSource source = gameplaySource(channel);
        Boolean accepted = GameplayDamageContext.call(
                source,
                () -> target.attackEntityFrom(DamageSource.causePlayerDamage(player), nativeAmount));
        return Boolean.TRUE.equals(accepted);
    }

    public static GameplayDamageSource gameplaySource(RuneDamageChannel channel) {
        if (channel == null) return null;
        switch (channel) {
            case CLICK:
                return GameplayDamageSource.CLICK;
            case POISON:
                return GameplayDamageSource.POISON;
            case BURNING:
                return GameplayDamageSource.BURNING;
            case BLEEDING:
                return GameplayDamageSource.BLEEDING;
            case CURSE:
                return GameplayDamageSource.CURSE;
            case WITHER:
                return GameplayDamageSource.WITHER;
            case DECAY:
                return GameplayDamageSource.DECAY;
            default:
                throw new IllegalArgumentException("Unsupported rune damage channel: " + channel);
        }
    }

    public static boolean isStatusChannel(RuneDamageChannel channel) {
        return channel != null && channel != RuneDamageChannel.CLICK;
    }
}
