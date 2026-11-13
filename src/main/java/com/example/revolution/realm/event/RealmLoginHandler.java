package com.example.revolution.realm.event;

import com.example.revolution.realm.service.RealmService;
import com.example.revolution.location.service.LocationResetReturnService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Login adapter for fresh least-populated Realm assignment. */
public final class RealmLoginHandler {
    private final RealmService realms;
    private final LocationResetReturnService locationReturn;

    public RealmLoginHandler(RealmService realms) { this(realms, null); }
    public RealmLoginHandler(RealmService realms, LocationResetReturnService locationReturn) {
        this.realms = realms;
        this.locationReturn = locationReturn;
    }

    @SubscribeEvent public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            realms.assignOnLogin(player);
            if (locationReturn != null) locationReturn.tryReturn(player);
        }
    }
}
