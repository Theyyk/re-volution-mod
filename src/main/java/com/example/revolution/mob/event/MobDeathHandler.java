package com.example.revolution.mob.event;

import com.example.revolution.RevolutionMod;
import com.example.revolution.lifecycle.ServerLifecycle;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RevolutionMod.MODID)
public class MobDeathHandler {

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) return;

        if (entity.getCustomNameTag() == null) return;
        if (!entity.getCustomNameTag().contains("Монстр монет")) return;

        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();

        ServerLifecycle.services().mobRewards.grantLegacyCoins(player);
    }
}
