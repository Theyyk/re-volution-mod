package com.example.revolution.prestige;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies resolved Prestige click effects after existing damage logic and before managed-mob consumption. */
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
        if (mobs.isApplyingPoisonDamage(player.getUniqueID())) return;
        BigDecimal multiplier = effects.multiplier(player.getUniqueID(), EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER);
        if (multiplier.compareTo(BigDecimal.ONE) == 0) return;
        event.setAmount((float) (event.getAmount() * multiplier.doubleValue()));
    }
}
