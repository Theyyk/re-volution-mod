package com.example.revolution.player.service;

import com.example.revolution.aura.AuraModifierService;
import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.god.GodWorshipModifierService;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.unique.UniqueRuneModifierService;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only normal-hit projection of the existing managed-mob damage layers.
 * The real LivingHurtEvent pipeline remains authoritative; no target, proc or randomness is simulated.
 */
public final class CombatDamageProjection implements PlayerStatsSyncService.DamageProjection {
    private final EffectResolver effects;
    private final UniqueRuneModifierService unique;
    private final GodWorshipModifierService gods;
    private final AuraModifierService auras;
    private final MechanicUnlockService unlocks;

    public CombatDamageProjection(EffectResolver effects, UniqueRuneModifierService unique,
                                  GodWorshipModifierService gods, AuraModifierService auras,
                                  MechanicUnlockService unlocks) {
        this.effects = effects;
        this.unique = unique;
        this.gods = gods;
        this.auras = auras;
        this.unlocks = unlocks;
    }

    @Override public PlayerStatsSyncService.DamagePreview project(UUID id, GameplayDamageSource source, double base) {
        BigDecimal damage = finite(base);
        StringBuilder details = new StringBuilder();
        details.append("Руны (обычный удар): ").append(format(damage));
        if (unlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS)) {
            BigDecimal next = unique.applyDamage(id, source, damage);
            append(details, "Уникальные руны", damage, next);
            damage = next;
        }
        EffectResolver.Stat channel = UniqueRuneModifierService.statFor(source);
        BigDecimal sourceFactor = effects.multiplier(id, channel);
        BigDecimal allFactor = effects.multiplier(id, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER);
        BigDecimal next = damage.multiply(allFactor).multiply(sourceFactor);
        append(details, "Эффекты (Все x" + format(allFactor) + ", канал x" + format(sourceFactor) + ")", damage, next);
        appendSources(details, id, EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER);
        appendSources(details, id, channel);
        damage = next;
        if (unlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.GOD_WORSHIP)) {
            next = gods.apply(id, source, damage, System.currentTimeMillis(),
                    unlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.GOD_SECONDARY_BUFFS));
            append(details, "Боги (активные)", damage, next);
            damage = next;
        }
        if (unlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.AURA_SYSTEM)) {
            next = auras.applyDamage(id, source, damage);
            append(details, "Аура", damage, next);
            damage = next;
        }
        details.append("\nБез случайных проков, критов и эффектов конкретного моба");
        return new PlayerStatsSyncService.DamagePreview(
                Math.max(0.0D, Math.min((double) Float.MAX_VALUE, damage.doubleValue())), details.toString());
    }

    private void appendSources(StringBuilder details, UUID id, EffectResolver.Stat stat) {
        for (EffectResolver.Contribution contribution : effects.inspect(id, stat)) {
            if (contribution == null || contribution.stat != stat || contribution.additiveMultiplier.signum() == 0) continue;
            String name = contribution.origin.startsWith("mastery:") ? "Mastery" :
                    contribution.origin.startsWith("pet") ? "Pets" : contribution.origin;
            details.append("\n  ").append(name).append(" [").append(stat.name()).append("] +")
                    .append(format(contribution.additiveMultiplier));
        }
    }

    private static void append(StringBuilder details, String name, BigDecimal before, BigDecimal after) {
        if (before.compareTo(after) == 0) return;
        details.append("\n").append(name).append(": ").append(format(after));
        if (before.signum() > 0) {
            BigDecimal factor = after.divide(before, 6, java.math.RoundingMode.HALF_UP);
            details.append(" (x").append(format(factor)).append(")");
        }
    }
    private static BigDecimal finite(double amount) {
        return BigDecimal.valueOf(Double.isFinite(amount) ? Math.max(0.0D, Math.min((double) Float.MAX_VALUE, amount)) : 0.0D);
    }
    private static String format(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
