package com.example.revolution.player.event;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameType;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    private static boolean isProtected(EntityPlayerMP player) {
        return player.interactionManager.getGameType() == GameType.SURVIVAL;
    }
}
