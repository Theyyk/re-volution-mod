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

    final Kind kind;
    final double primaryValue;
    final double secondaryValue;
    final RuneStatType damageType;

    private RuneCompositeEffect(Kind kind,
                                double primaryValue,
                                double secondaryValue,
                                RuneStatType damageType) {
        if (kind == null) throw new IllegalArgumentException("Composite effect kind is required");
        if (!Double.isFinite(primaryValue) || !Double.isFinite(secondaryValue)) {
            throw new IllegalArgumentException("Composite effect values must be finite");
        }
        this.kind = kind;
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.damageType = damageType;
    }

    static RuneCompositeEffect clickDamageProc(double chancePercent, double damageMultiplier) {
        if (chancePercent < 0.0D || damageMultiplier < 0.0D) {
            throw new IllegalArgumentException("Proc values cannot be negative");
        }
        return new RuneCompositeEffect(Kind.CLICK_DAMAGE_PROC, chancePercent, damageMultiplier, null);
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
                damageMultiplierType
        );
    }

    static RuneCompositeEffect temporaryAllDamage(double multiplier) {
        if (multiplier < 0.0D) throw new IllegalArgumentException("Temporary damage multiplier cannot be negative");
        return new RuneCompositeEffect(Kind.TEMPORARY_ALL_DAMAGE_MULTIPLIER, multiplier, 0.0D, null);
    }

    static RuneCompositeEffect enhancementsMoreEffective() {
        return new RuneCompositeEffect(Kind.ENHANCEMENTS_MORE_EFFECTIVE, 0.0D, 0.0D, null);
    }

    List<String> tooltipLines() {
        List<String> lines = new ArrayList<>();
        switch (kind) {
            case CLICK_DAMAGE_PROC:
                lines.add(RuneStatType.compact(primaryValue) + "% шанс нанести");
                lines.add("×" + RuneStatType.compact(secondaryValue) + " урона при клике");
                break;
            case DELAYED_DAMAGE_MULTIPLIER:
                lines.add("Через " + RuneStatType.compact(primaryValue) + " сек:");
                lines.add(damageType.displayName + " ×" + RuneStatType.compact(secondaryValue));
                break;
            case TEMPORARY_ALL_DAMAGE_MULTIPLIER:
                lines.add("Клики временно увеличивают");
                lines.add("Весь урон ×" + RuneStatType.compact(primaryValue));
                break;
            case ENHANCEMENTS_MORE_EFFECTIVE:
                lines.add("Усиления эффективнее");
                break;
            default:
                return Collections.emptyList();
        }
        return lines;
    }
}
