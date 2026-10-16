package com.example.customguimod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured rune effects that cannot be represented by a single scalar stat.
 * Tooltip lines are intentionally compact and may span multiple rows.
 */
final class RuneCompositeEffect {
    enum Kind {
        CLICK_DAMAGE_PROC,
        DELAYED_DAMAGE_MULTIPLIER,
        TEMPORARY_ALL_DAMAGE_MULTIPLIER,
        ENHANCEMENTS_MORE_EFFECTIVE
    }

    enum Activation {
        NONE,
        CLICK_HIT_MOB
    }

    final Kind kind;
    final double primaryValue;
    final double secondaryValue;
    final double durationSeconds;
    final RuneStatType damageType;
    final Activation activation;
    final int maxStacks;

    private RuneCompositeEffect(Kind kind,
                                double primaryValue,
                                double secondaryValue,
                                double durationSeconds,
                                RuneStatType damageType,
                                Activation activation,
                                int maxStacks) {
        if (kind == null) throw new IllegalArgumentException("Composite effect kind is required");
        if (!Double.isFinite(primaryValue) || !Double.isFinite(secondaryValue) || !Double.isFinite(durationSeconds)) {
            throw new IllegalArgumentException("Composite effect values must be finite");
        }
        if (durationSeconds < 0.0D) throw new IllegalArgumentException("Composite effect duration cannot be negative");
        if (activation == null) throw new IllegalArgumentException("Composite effect activation is required");
        if (maxStacks < 0) throw new IllegalArgumentException("Composite effect max stacks cannot be negative");
        this.kind = kind;
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.durationSeconds = durationSeconds;
        this.damageType = damageType;
        this.activation = activation;
        this.maxStacks = maxStacks;
    }

    static RuneCompositeEffect clickDamageProc(double chancePercent, double damageMultiplier) {
        if (chancePercent < 0.0D || damageMultiplier < 0.0D) {
            throw new IllegalArgumentException("Proc values cannot be negative");
        }
        return new RuneCompositeEffect(
                Kind.CLICK_DAMAGE_PROC,
                chancePercent,
                damageMultiplier,
                0.0D,
                null,
                Activation.CLICK_HIT_MOB,
                0
        );
    }

    static RuneCompositeEffect delayedDamage(double delaySeconds,
                                             RuneStatType damageMultiplierType,
                                             double multiplier) {
        if (delaySeconds < 0.0D || multiplier < 0.0D) {
            throw new IllegalArgumentException("Delayed damage values cannot be negative");
        }
        if (damageMultiplierType == null || !damageMultiplierType.isDamageMultiplier()) {
            throw new IllegalArgumentException("Delayed damage requires a channel damage multiplier type");
        }
        return new RuneCompositeEffect(
                Kind.DELAYED_DAMAGE_MULTIPLIER,
                delaySeconds,
                multiplier,
                0.0D,
                damageMultiplierType,
                Activation.NONE,
                0
        );
    }

    static RuneCompositeEffect temporaryAllDamage(double multiplier, double durationSeconds, int maxStacks) {
        if (multiplier < 0.0D) throw new IllegalArgumentException("Temporary damage multiplier cannot be negative");
        if (durationSeconds <= 0.0D) throw new IllegalArgumentException("Temporary all-damage effect needs a positive duration");
        if (maxStacks < 1) throw new IllegalArgumentException("Temporary all-damage effect must allow at least one stack");
        return new RuneCompositeEffect(
                Kind.TEMPORARY_ALL_DAMAGE_MULTIPLIER,
                multiplier,
                0.0D,
                durationSeconds,
                null,
                Activation.CLICK_HIT_MOB,
                maxStacks
        );
    }

    static RuneCompositeEffect enhancementsMoreEffective() {
        return new RuneCompositeEffect(
                Kind.ENHANCEMENTS_MORE_EFFECTIVE,
                0.0D,
                0.0D,
                0.0D,
                null,
                Activation.NONE,
                0
        );
    }

    double resolvedProcDamageMultiplier(int rank, double enhancementMultiplier) {
        return kind == Kind.CLICK_DAMAGE_PROC
                ? scaledDamageValue(secondaryValue, rank, enhancementMultiplier)
                : secondaryValue;
    }

    double resolvedTemporaryAllDamageMultiplier(int rank, double enhancementMultiplier) {
        return kind == Kind.TEMPORARY_ALL_DAMAGE_MULTIPLIER
                ? scaledDamageValue(primaryValue, rank, enhancementMultiplier)
                : primaryValue;
    }

    double resolvedDelayedDamageMultiplier(int rank, double enhancementMultiplier) {
        return kind == Kind.DELAYED_DAMAGE_MULTIPLIER
                ? scaledDamageValue(secondaryValue, rank, enhancementMultiplier)
                : secondaryValue;
    }

    List<String> tooltipLines() {
        return tooltipLines(RuneInventory.NORMAL_RANK, 1.0D);
    }

    List<String> tooltipLines(int rank, double enhancementMultiplier) {
        List<String> lines = new ArrayList<>();
        switch (kind) {
            case CLICK_DAMAGE_PROC:
                lines.add(RuneStatType.compact(primaryValue) + "% шанс нанести");
                lines.add("x" + RuneStatType.compact(resolvedProcDamageMultiplier(rank, enhancementMultiplier))
                        + " урона при клике");
                break;
            case DELAYED_DAMAGE_MULTIPLIER:
                lines.add("Через " + RuneStatType.compact(primaryValue) + " сек:");
                lines.add(damageType.displayName + " x"
                        + RuneStatType.compact(resolvedDelayedDamageMultiplier(rank, enhancementMultiplier)));
                break;
            case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                lines.add("Попадание кликом увеличивает");
                lines.add("Весь урон x"
                        + RuneStatType.compact(resolvedTemporaryAllDamageMultiplier(rank, enhancementMultiplier))
                        + " на " + RuneStatType.compact(durationSeconds) + " сек");
                break;
            case ENHANCEMENTS_MORE_EFFECTIVE:
                lines.add("Усиления эффективнее");
                break;
            default:
                return Collections.emptyList();
        }
        return lines;
    }

    private static double scaledDamageValue(double baseValue, int rank, double enhancementMultiplier) {
        double rankMultiplier = rank >= RuneInventory.ADVANCED_RANK ? 9.0D
                : rank >= RuneInventory.IMPROVED_RANK ? 3.0D
                : 1.0D;
        double safeEnhancementMultiplier = Double.isFinite(enhancementMultiplier)
                ? Math.max(1.0D, enhancementMultiplier)
                : 1.0D;
        return Math.max(0.0D, baseValue) * rankMultiplier * safeEnhancementMultiplier;
    }
}
