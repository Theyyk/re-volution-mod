package com.example.customguimod;


import com.example.customguimod.rune.RuneCombatMath;
import com.example.customguimod.rune.RuneCombatService;
import com.example.customguimod.mob.MobManager;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageBonusHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLivingBase)) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        if (MobManager.isApplyingPoisonDamage(player.getUniqueID())) return;

        // The v1.2.10 rune combat model is PvE-only: rage/proc are explicitly triggered by a mob hit.
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            long worldTick = player.getServerWorld().getTotalWorldTime();
            RuneCombatMath.Snapshot snapshot = RuneCombatService.resolve(player.getUniqueID(), worldTick);
            if (snapshot != null) {
                boolean procTriggered = RuneCombatService.rollProc(snapshot);
                double damage = RuneCombatMath.clickDamage(event.getAmount(), snapshot, procTriggered);
                event.setAmount((float) damage);

                // Rage starts after the triggering hit, so this hit itself does not receive a newly-created buff.
                RuneCombatService.activateRageOnMobHit(player.getUniqueID(), worldTick);
                return;
            }
        }

        // Legacy/non-rune statuses keep their established additive damage behavior.
        double totalDamageBonus = PlayerStatsService.getTotalDamageBonusValue(player.getUniqueID());
        if (totalDamageBonus <= 0.0D) return;
        event.setAmount(event.getAmount() + (float) totalDamageBonus);
    }
}
