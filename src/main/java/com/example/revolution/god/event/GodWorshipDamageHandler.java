package com.example.revolution.god.event;

import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModifierService;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies the isolated God multiplier after normal effects and before managed-mob damage consumption. */
public final class GodWorshipDamageHandler {
    private final ManagedMobService mobs;
    private final GodWorshipModifierService modifiers;

    public GodWorshipDamageHandler(ManagedMobService mobs, GodWorshipModifierService modifiers) {
        if (mobs == null || modifiers == null) throw new IllegalArgumentException("God damage handler dependencies are required");
        this.mobs = mobs;
        this.modifiers = modifiers;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!mobs.manages(event.getEntityLiving())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        GameplayDamageSource source = GameplayDamageContext.current();
        if (source == null) {
            source = mobs.isApplyingPoisonDamage(player.getUniqueID())
                    ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK;
        }
        BigDecimal before = BigDecimal.valueOf(event.getAmount());
        BigDecimal after = modifiers.apply(player.getUniqueID(), source, before, System.currentTimeMillis());
        if (after.compareTo(before) != 0) event.setAmount(after.floatValue());
    }
}
