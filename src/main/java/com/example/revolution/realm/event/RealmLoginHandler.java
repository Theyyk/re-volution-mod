package com.example.revolution.realm.event;

import com.example.revolution.realm.service.RealmService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Login adapter for fresh least-populated Realm assignment. */
public final class RealmLoginHandler {
    private final RealmService realms;

    public RealmLoginHandler(RealmService realms) { this.realms = realms; }

    @SubscribeEvent public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) realms.assignOnLogin((EntityPlayerMP) event.player);
    }
}
