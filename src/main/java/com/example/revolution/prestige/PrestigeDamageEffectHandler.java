package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.unique.UniqueRuneModifierService;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies resolved Prestige damage effects after existing damage logic and before managed-mob consumption. */
public final class PrestigeDamageEffectHandler {
    private final ManagedMobService mobs;
    private final EffectResolver effects;

    public PrestigeDamageEffectHandler(ManagedMobService mobs, EffectResolver effects) {
        this.mobs = mobs;
        this.effects = effects;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!mobs.manages(event.getEntityLiving())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        BigDecimal multiplier = damageMultiplier(effects, player.getUniqueID(), GameplayDamageContext.current(),
                mobs.isApplyingPoisonDamage(player.getUniqueID()));
        if (multiplier.compareTo(BigDecimal.ONE) == 0) return;
        event.setAmount((float) (event.getAmount() * multiplier.doubleValue()));
    }

    /** Match the existing channel mapping used by Unique Rune and E projection.
     * Legacy hits without an explicit context retain their Click/Poison classification.
     * Unmapped channels receive only ALL_DAMAGE rather than an unrelated Click bonus.
     */
    static BigDecimal damageMultiplier(EffectResolver effects, UUID playerUuid,
                                       GameplayDamageSource context, boolean legacyPoison) {
        GameplayDamageSource source = context == null
                ? (legacyPoison ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK) : context;
        EffectResolver.Stat channel = UniqueRuneModifierService.statFor(source);
        BigDecimal multiplier = effects.multiplier(playerUuid, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER);
        return channel == null ? multiplier : multiplier.multiply(effects.multiplier(playerUuid, channel));
    }
}
