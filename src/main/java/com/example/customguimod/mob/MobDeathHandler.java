package com.example.customguimod.mob;

import com.example.customguimod.CustomGuiMod;
import com.example.customguimod.network.NetworkHandler;
import com.example.customguimod.network.PongPacket;
import com.example.customguimod.persistence.MongoManager;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CustomGuiMod.MODID)
public class MobDeathHandler {

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null) return;

        if (entity.getCustomNameTag() == null) return;
        if (!entity.getCustomNameTag().contains("Монстр монет")) return;

        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();

        int balance = MongoManager.getBalance(player.getUniqueID());
        MongoManager.setBalance(player.getUniqueID(), balance + 50);

        player.sendMessage(new TextComponentString(
                "§6+50 монет! Баланс: " + (balance + 50)));
        NetworkHandler.INSTANCE.sendTo(new PongPacket(balance + 50), player);
    }
}
