package com.example.revolution.achievement;

import com.example.revolution.network.AchievementRequestLimiter;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Flushes buffered high-frequency progress without writing Mongo on every damage/resource event. */
public final class AchievementPersistenceHandler {
    private static final int FLUSH_INTERVAL_TICKS = 20 * 30;
    private final AchievementService achievements;
    private int ticks;

    public AchievementPersistenceHandler(AchievementService achievements) {
        if (achievements == null) throw new IllegalArgumentException("achievements is required");
        this.achievements = achievements;
    }

    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        achievements.unload(event.player.getUniqueID());
        AchievementRequestLimiter.forget(event.player.getUniqueID());
    }

    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++ticks < FLUSH_INTERVAL_TICKS) return;
        ticks = 0;
        achievements.flushAll();
    }
}
