package com.example.revolution.combat;

import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.player.service.PlayerStatsService;
import com.example.revolution.rune.rules.RuneCombatMath;
import com.example.revolution.rune.service.RuneCombatService;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageBonusHandler {
    private final ManagedMobService mobs;
    private final RuneCombatService combat;
    private final PlayerStatsService stats;

    public DamageBonusHandler(ManagedMobService mobs, RuneCombatService combat, PlayerStatsService stats) {
        this.mobs = mobs;
        this.combat = combat;
        this.stats = stats;
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLivingBase)) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        if (mobs.isApplyingPoisonDamage(player.getUniqueID())) return;

        // The v1.2.10 rune combat model is PvE-only: rage/proc are explicitly triggered by a mob hit.
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            long worldTick = player.getServerWorld().getTotalWorldTime();
            RuneCombatMath.Snapshot snapshot = combat.resolve(player.getUniqueID(), worldTick);
            if (snapshot != null) {
                boolean procTriggered = combat.rollProc(snapshot);
                double damage = RuneCombatMath.clickDamage(event.getAmount(), snapshot, procTriggered);
                event.setAmount((float) damage);

                // Rage starts after the triggering hit, so this hit itself does not receive a newly-created buff.
                combat.activateRageOnMobHit(player.getUniqueID(), worldTick);
                return;
            }
        }

        // Legacy/non-rune statuses keep their established additive damage behavior.
        double totalDamageBonus = stats.getTotalDamageBonusValue(player.getUniqueID());
        if (totalDamageBonus <= 0.0D) return;
        event.setAmount(event.getAmount() + (float) totalDamageBonus);
    }
}
