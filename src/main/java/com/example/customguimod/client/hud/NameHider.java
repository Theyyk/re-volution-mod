package com.example.customguimod.client.hud;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NameHider {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderSpecials(RenderLivingEvent.Specials.Pre event) {
        EntityLivingBase entity = event.getEntity();
        if (!entity.hasCustomName()) return;

        String name = entity.getCustomNameTag();
        if (name.startsWith("§c") && name.contains("|")) {
            event.setCanceled(true);
        }
    }
}
