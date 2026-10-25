package com.example.revolution.mob.event;

import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.mob.appearance.MobAppearanceCatalog;
import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import com.example.revolution.mob.appearance.MobVisualWire;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds the stable visual slot marker and player-facing Russian name before managed entities are tracked to clients. */
public final class ManagedMobAppearanceEvents {
    private static final String INTERNAL_LOCATION_ID_TAG = "revolutionLocationId";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return;
        if (!entity.hasCustomName()) return;

        NBTTagCompound data = entity.getEntityData();
        if (!data.hasKey(INTERNAL_LOCATION_ID_TAG, 3)) return;
        int internalLocationId = data.getInteger(INTERNAL_LOCATION_ID_TAG);
        if (!MobZoneIds.isMobZoneId(internalLocationId)) return;
        int locationId = MobZoneIds.worldLocationId(internalLocationId);
        int mobSlotId = MobZoneIds.mobSlotId(internalLocationId);
        MobAppearanceDefinition appearance = MobAppearanceCatalog.bySlot(locationId, mobSlotId);
        if (appearance == null) return;

        String raw = entity.getCustomNameTag();
        if (raw == null || !raw.startsWith("§c") || !raw.contains("|")) return;
        String[] parts = raw.substring(2).split("\\|", -1);
        if (parts.length < 5) return;
        entity.setCustomNameTag("§c" + appearance.getDisplayName()
                + "|" + parts[1]
                + "|" + parts[2]
                + "|" + parts[3]
                + "|" + parts[4]
                + "|" + MobVisualWire.encode(locationId, mobSlotId));
    }
}
