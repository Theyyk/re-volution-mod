package com.example.revolution.mob.event;

import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.mob.appearance.MobAppearanceCatalog;
import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import com.example.revolution.mob.appearance.MobAppearanceSizing;
import com.example.revolution.mob.appearance.MobVisualWire;
import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Keeps the stable visual slot marker, physical bounds and player-facing Russian name on managed Location 1 entities. */
public final class ManagedMobAppearanceEvents {
    private static final String INTERNAL_LOCATION_ID_TAG = "revolutionLocationId";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote) refreshNow(event.getEntity());
    }

    /**
     * ManagedMobService can rewrite its health payload after hits and chunk recovery. The regular end-of-tick
     * pass remains a recovery guard, while combat adapters call refreshNow immediately after damage so the
     * technical mob ID never survives until entity metadata is flushed to clients.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (World world : DimensionManager.getWorlds()) {
            if (world == null || world.isRemote) continue;
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) refreshNow(entity);
        }
    }

    public static void refreshNow(Entity entity) {
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

        MobAppearanceSizing.apply((net.minecraft.entity.EntityLivingBase) entity, appearance);

        String raw = entity.getCustomNameTag();
        if (raw == null || !raw.startsWith("§c") || !raw.contains("|")) return;
        String[] parts = raw.substring(2).split("\\|", -1);
        if (parts.length < 5) return;
        String expected = MobVisualWire.displayNamePayload(appearance, parts[1], parts[2], parts[3], parts[4]);
        if (!expected.equals(raw)) entity.setCustomNameTag(expected);
    }
}
