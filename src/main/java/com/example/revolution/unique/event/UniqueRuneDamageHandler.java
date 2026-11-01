package com.example.revolution.unique.event;

import com.example.revolution.gameplay.GameplayDamageContext;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.unique.UniqueRuneModifierService;
import java.math.BigDecimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** UNIQUE_RUNE damage layer. Runs before the dedicated GOD and AURA LOW-priority layers. */
public final class UniqueRuneDamageHandler {
    private final ManagedMobService mobs;
    private final UniqueRuneModifierService modifiers;
    private final MechanicUnlockService unlocks;
    public UniqueRuneDamageHandler(ManagedMobService mobs, UniqueRuneModifierService modifiers, MechanicUnlockService unlocks) {
        if (mobs == null || modifiers == null || unlocks == null) throw new IllegalArgumentException("Unique Rune damage dependencies are required");
        this.mobs = mobs; this.modifiers = modifiers; this.unlocks = unlocks;
    }
    @SubscribeEvent(priority = EventPriority.NORMAL) public void onLivingHurt(LivingHurtEvent event) {
        if (!mobs.manages(event.getEntityLiving())) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        if (!unlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)) return;
        GameplayDamageSource source = GameplayDamageContext.current();
        if (source == null) source = mobs.isApplyingPoisonDamage(player.getUniqueID()) ? GameplayDamageSource.POISON : GameplayDamageSource.CLICK;
        BigDecimal before = BigDecimal.valueOf(event.getAmount());
        BigDecimal after = modifiers.applyDamage(player.getUniqueID(), source, before);
        if (after.compareTo(before) != 0) event.setAmount(after.floatValue());
    }
}
