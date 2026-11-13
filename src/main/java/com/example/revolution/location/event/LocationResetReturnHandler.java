package com.example.revolution.location.event;

import com.example.revolution.location.service.LocationResetReturnService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Resume a committed-but-not-acknowledged return without invoking Reset again. */
public final class LocationResetReturnHandler {
    private final LocationResetReturnService returns;
    public LocationResetReturnHandler(LocationResetReturnService returns) { this.returns = returns; }

    @SubscribeEvent public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)
                || event.player.world.isRemote || event.player.world.getTotalWorldTime() % 200L != 0L) return;
        returns.tryReturn((EntityPlayerMP) event.player);
    }
}
