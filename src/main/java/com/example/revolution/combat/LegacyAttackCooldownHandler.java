package com.example.revolution.combat;

import com.example.revolution.mob.service.ManagedMobService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Restores pre-1.9 click cadence for RE:volution managed-mob melee damage only. */
public final class LegacyAttackCooldownHandler {
    private final ManagedMobService mobs;
    private final Map<UUID, PendingAttack> pending = new HashMap<>();

    public LegacyAttackCooldownHandler(ManagedMobService mobs) {
        if (mobs == null) throw new IllegalArgumentException("managed mob service is required");
        this.mobs = mobs;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (!mobs.manages(target)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        double attackDamage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        double enchantmentDamage = EnchantmentHelper.getModifierForCreature(
                player.getHeldItemMainhand(), target.getCreatureAttribute());
        pending.put(player.getUniqueID(), new PendingAttack(
                target.getEntityId(),
                player.getServerWorld().getTotalWorldTime(),
                player.getCooledAttackStrength(0.5F),
                attackDamage,
                enchantmentDamage));
    }

    /**
     * Vanilla 1.12.2 applies target hurt-resistance before LivingHurtEvent. A rapid
     * melee hit can therefore be discarded before cooldown normalization has a
     * chance to run. Clear only the managed target's current i-frame window for
     * the exact primary melee attack captured above so every click reaches the
     * normal damage pipeline.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        if (!mobs.manages(event.getEntityLiving())) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        if (event.getSource().getImmediateSource() != player) return;

        PendingAttack attack = pending.get(player.getUniqueID());
        if (attack == null) return;
        if (attack.targetEntityId != event.getEntityLiving().getEntityId()) return;
        if (attack.worldTick != player.getServerWorld().getTotalWorldTime()) return;

        event.getEntityLiving().hurtResistantTime = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayerMP)) return;
        if (!mobs.manages(event.getEntityLiving())) return;

        EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
        PendingAttack attack = pending.remove(player.getUniqueID());
        if (attack == null) return;
        if (attack.targetEntityId != event.getEntityLiving().getEntityId()) return;
        if (attack.worldTick != player.getServerWorld().getTotalWorldTime()) return;

        double normalized = LegacyAttackCooldownMath.normalizeToFullStrength(
                event.getAmount(),
                attack.attackDamage,
                attack.enchantmentDamage,
                attack.cooledAttackStrength);
        if (!Double.isFinite(normalized) || normalized < 0.0D) return;
        event.setAmount((float) Math.min((double) Float.MAX_VALUE, normalized));
    }

    private static final class PendingAttack {
        final int targetEntityId;
        final long worldTick;
        final double cooledAttackStrength;
        final double attackDamage;
        final double enchantmentDamage;

        PendingAttack(int targetEntityId, long worldTick, double cooledAttackStrength,
                double attackDamage, double enchantmentDamage) {
            this.targetEntityId = targetEntityId;
            this.worldTick = worldTick;
            this.cooledAttackStrength = cooledAttackStrength;
            this.attackDamage = attackDamage;
            this.enchantmentDamage = enchantmentDamage;
        }
    }
}
