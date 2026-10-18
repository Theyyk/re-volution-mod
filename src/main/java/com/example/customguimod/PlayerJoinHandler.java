package com.example.customguimod;

import com.example.customguimod.persistence.MongoManager;
import com.example.customguimod.resource.ResourceManager;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class PlayerJoinHandler {

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        java.util.UUID uuid = player.getUniqueID();

        // Создаём основную колоду, если её нет
        if (MongoManager.getCollectionNames(uuid).isEmpty()) {
            MongoManager.addCollection(uuid, "Основная");
            CustomGuiMod.logger.info("Created default deck for " + player.getName());
        }

        // v1.2.2: полностью удаляем устаревшее поле lightnings,
        // в том числе у новых документов, созданных старой схемой MongoManager.
        ResourceManager.purgeDeprecatedLightnings(uuid);

        PlayerStatsService.sendTo(player);
        CustomGuiMod.logger.info("Player joined: " + player.getName());
    }
}
