package com.example.revolution.aura.event;

import com.example.revolution.aura.AuraModifierService;
import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Registered after GodWorshipDamageHandler so AURA is the final dedicated damage layer. */
public final class AuraDamageHandler {
    private final ManagedMobService mobs;
    private final AuraModifierService modifiers;

    public AuraDamageHandler(ManagedMobService mobs, AuraModifierService modifiers) {
        if (mobs == null || modifiers == null) throw new IllegalArgumentException("Aura damage handler dependencies are required");
        this.mobs = mobs;
        this.modifiers = modifiers;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!mobs.manages(event.getEntityLiving())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        GameplayDamageSource source = GameplayDamageContext.current();
        if (source == null) source = mobs.isApplyingPoisonDamage(player.getUniqueID()) ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK;
        BigDecimal before = BigDecimal.valueOf(event.getAmount());
        BigDecimal after = modifiers.applyDamage(player.getUniqueID(), source, before);
        if (after.compareTo(before) != 0) event.setAmount(after.floatValue());
    }
}
