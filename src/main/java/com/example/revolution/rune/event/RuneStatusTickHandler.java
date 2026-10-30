package com.example.revolution.rune.event;

import com.example.revolution.mob.rules.MobHealthRules;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.rules.RunePoisonAoeRules;
import com.example.revolution.rune.service.RuneCombatService;
import com.example.revolution.rune.service.RuneStatusDamageRuntime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Functional v2.3 runtime for the five new rune status channels.
 *
 * <p>POISON deliberately stays in ManagedMobService so Collection 1 keeps its established runtime
 * behavior. Until per-status timed policies are promoted into Balance Core, the new technical
 * channels reuse the existing status radius/cadence and AoE budget distribution.</p>
 */
public final class RuneStatusTickHandler {
    private static final RuneDamageChannel[] RUNTIME_CHANNELS = {
            RuneDamageChannel.BURNING,
            RuneDamageChannel.BLEEDING,
            RuneDamageChannel.CURSE,
            RuneDamageChannel.WITHER,
            RuneDamageChannel.DECAY
    };

    private final ManagedMobService mobs;
    private final RuneCombatService combat;
    private int tickCounter;

    public RuneStatusTickHandler(ManagedMobService mobs, RuneCombatService combat) {
        if (mobs == null || combat == null) throw new IllegalArgumentException("status runtime dependencies are required");
        this.mobs = mobs;
        this.combat = combat;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < RunePoisonAoeRules.MC_TICK_INTERVAL) return;
        tickCounter = 0;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            applyPlayerStatuses(player);
        }
    }

    private void applyPlayerStatuses(EntityPlayerMP player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        RuneDamageMath.Snapshot snapshot = combat.resolveDamage(player.getUniqueID(), player.world.getTotalWorldTime());
        if (snapshot == null) return;

        List<EntityLivingBase> targets = managedTargets(player);
        if (targets.isEmpty()) return;

        for (RuneDamageChannel channel : RUNTIME_CHANNELS) {
            double statusDps = RuneDamageMath.statusDamage(channel, snapshot);
            double perTargetTick = RunePoisonAoeRules.perTargetTick(statusDps, targets.size());
            if (perTargetTick <= 0.0D) continue;

            for (EntityLivingBase target : targets) {
                if (target == null || target.isDead) continue;
                RuneStatusDamageRuntime.apply(player, target, channel, perTargetTick);
            }
        }
    }

    private List<EntityLivingBase> managedTargets(EntityPlayerMP player) {
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Entity entity : new ArrayList<>(player.world.loadedEntityList)) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isDead || !mobs.manages(living)) continue;
            if (!MobHealthRules.isWithinStatusRadiusSq(player.getDistanceSq(living))) continue;
            targets.add(living);
        }
        return targets;
    }
}
