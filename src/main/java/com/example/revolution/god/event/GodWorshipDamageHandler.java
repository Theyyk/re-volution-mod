package com.example.revolution.god.event;

import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModifierService;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.mob.service.ManagedMobService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Applies God modifiers only after the canonical God Worship parent unlock. */
public final class GodWorshipDamageHandler {
    private final ManagedMobService mobs;
    private final GodWorshipModifierService modifiers;
    private final MechanicUnlockService unlocks;
    public GodWorshipDamageHandler(ManagedMobService mobs, GodWorshipModifierService modifiers, MechanicUnlockService unlocks) {
        if (mobs == null || modifiers == null || unlocks == null) throw new IllegalArgumentException("God damage handler dependencies are required");
        this.mobs = mobs; this.modifiers = modifiers; this.unlocks = unlocks;
    }
    @SubscribeEvent(priority = EventPriority.LOW) public void onLivingHurt(LivingHurtEvent event) {
        if (!mobs.manages(event.getEntityLiving())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        if (!unlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_WORSHIP)) return;
        GameplayDamageSource source = GameplayDamageContext.current();
        if (source == null) source = mobs.isApplyingPoisonDamage(player.getUniqueID()) ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK;
        BigDecimal before = BigDecimal.valueOf(event.getAmount());
        BigDecimal after = modifiers.apply(player.getUniqueID(), source, before, System.currentTimeMillis());
        if (after.compareTo(before) != 0) event.setAmount(after.floatValue());
    }
}
