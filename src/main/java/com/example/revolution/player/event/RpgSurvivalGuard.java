package com.example.revolution.player.event;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameType;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Server-authoritative RPG rules for players that remain in Survival mode. */
public final class RpgSurvivalGuard {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttacked(LivingAttackEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP
                && isProtected((EntityPlayerMP) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP
                && isProtected((EntityPlayerMP) event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP
                && isProtected((EntityPlayerMP) event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (!isProtected(player)) return;

        player.getFoodStats().setFoodLevel(20);
        player.getFoodStats().setFoodSaturationLevel(20.0F);
    }

    private static boolean isProtected(EntityPlayerMP player) {
        return player.interactionManager.getGameType() == GameType.SURVIVAL;
    }
}
