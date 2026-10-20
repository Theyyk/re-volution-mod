package com.example.revolution.realm.event;

import com.example.revolution.realm.definition.RealmCatalog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Copied entity NBT still names DIM201; bind loaded entities to their actual Realm. */
public final class RealmEntityLoadHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityLoad(EntityJoinWorldEvent event) {
        int dimension = event.getWorld().provider.getDimension();
        if (!event.getWorld().isRemote && RealmCatalog.isRealmDimension(dimension)
                && !(event.getEntity() instanceof EntityPlayer)) {
            event.getEntity().dimension = dimension;
        }
    }
}
