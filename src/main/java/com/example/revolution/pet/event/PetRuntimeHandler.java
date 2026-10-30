package com.example.revolution.pet.event;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.pet.service.PetEffectSource;
import com.example.revolution.pet.service.PetService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Online-only Pet XP plus server-authoritative autoclick. The client never reports generated hits. */
public final class PetRuntimeHandler {
    private static final long XP_FLUSH_MS = 1000L;
    private static final double AUTOCLICK_REACH = 5.0D;

    private final PetService pets;
    private final PetEffectSource petEffects;
    private final EffectResolver effects;
    private final ManagedMobService mobs;
    private final Map<UUID, Long> lastXpAt = new HashMap<>();
    private final Map<UUID, Long> nextAutoClickAt = new HashMap<>();

    public PetRuntimeHandler(PetService pets, PetEffectSource petEffects, EffectResolver effects, ManagedMobService mobs) {
        this.pets = pets;
        this.petEffects = petEffects;
        this.effects = effects;
        this.mobs = mobs;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID playerId = player.getUniqueID();
        long now = System.currentTimeMillis();

        Long previous = lastXpAt.get(playerId);
        if (previous == null) {
            lastXpAt.put(playerId, now);
            pets.markOnline(playerId); // Explicitly discard offline elapsed time.
        } else if (now - previous >= XP_FLUSH_MS) {
            long elapsed = now - previous;
            lastXpAt.put(playerId, now);
            pets.accrueOnlineXp(playerId, elapsed, effects.multiplier(playerId, EffectResolver.Stat.PET_XP_MULTIPLIER));
        }

        BigDecimal cps = petEffects.autoClickCps(playerId);
        if (cps.signum() <= 0) {
            nextAutoClickAt.remove(playerId);
            return;
        }
        long interval = Math.max(50L, BigDecimal.valueOf(1000L).divide(cps, 0, java.math.RoundingMode.CEILING).longValue());
        long due = nextAutoClickAt.containsKey(playerId) ? nextAutoClickAt.get(playerId) : now;
        if (now < due) return;
        nextAutoClickAt.put(playerId, now + interval);

        EntityLivingBase target = managedTargetUnderCrosshair(player, AUTOCLICK_REACH);
        if (target != null) player.attackTargetEntityWithCurrentItem(target);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        UUID playerId = event.player.getUniqueID();
        lastXpAt.remove(playerId);
        nextAutoClickAt.remove(playerId);
    }

    private EntityLivingBase managedTargetUnderCrosshair(EntityPlayerMP player, double reach) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = start.addVector(look.x * reach, look.y * reach, look.z * reach);
        AxisAlignedBB scan = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach).grow(1.0D);
        List<Entity> candidates = player.world.getEntitiesWithinAABBExcludingEntity(player, scan);
        EntityLivingBase best = null;
        double bestDistance = reach;
        for (Entity candidate : candidates) {
            if (!(candidate instanceof EntityLivingBase) || !candidate.canBeCollidedWith() || candidate.isDead || !mobs.manages((EntityLivingBase) candidate)) continue;
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(candidate.getCollisionBorderSize());
            RayTraceResult intercept = box.calculateIntercept(start, end);
            if (box.contains(start)) {
                if (bestDistance >= 0.0D) { best = (EntityLivingBase) candidate; bestDistance = 0.0D; }
            } else if (intercept != null) {
                double distance = start.distanceTo(intercept.hitVec);
                if (distance < bestDistance) { best = (EntityLivingBase) candidate; bestDistance = distance; }
            }
        }
        return best;
    }
}
