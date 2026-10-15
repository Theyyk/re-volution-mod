package com.example.customguimod;

import java.util.Arrays;
import java.util.List;

final class RuneStatContentTest {
    static void run() {
        check(RuneStatType.values().length == 30, "v1.2.10 scalar stat catalog should contain 30 reference-backed types");

        check(RuneStatType.CLICK_DAMAGE_FLAT.format(12.5D).equals("+12.5"), "Flat click damage formatting changed");
        check(RuneStatType.POISON_DAMAGE_MULTIPLIER.format(3.0D).equals("×3"), "Damage multiplier formatting changed");
        check(RuneStatType.POISON_VULNERABILITY.format(25.0D).equals("+25%"), "Vulnerability should be percent-based");
        check(RuneStatType.CLICK_CRIT_CHANCE.format(9.0D).equals("+9%"), "Crit chance should be percent-based");
        check(RuneStatType.CLICK_CRIT_DAMAGE_MULTIPLIER.format(5.1D).equals("×5.1"), "Crit damage should be multiplier-based");
        check(RuneStatType.MAGIC_SPHERE_DURATION.format(3.0D).equals("+3 сек"), "Sphere duration should render seconds");
        check(RuneStatType.COIN_REWARD_PERCENT.displayName.equals("Добыча монет"), "Coin reward label changed");
        check(RuneStatType.RESOURCE_REWARD_PERCENT.displayName.equals("Добыча ресурса"), "Resource reward label changed");

        for (RuneStatType type : RuneStatType.values()) {
            check(!type.name().endsWith("_EFFECTIVENESS"), "Per-channel effectiveness was not supported by the references");
            check(!type.name().equals("MONSTER_GOLD_MULTIPLIER"), "Old gold reference alias should not return");
            check(!type.name().equals("MONSTER_RESOURCE_MULTIPLIER"), "Old resource reference alias should not return");
        }

        List<RuneStatType> channelMultipliers = Arrays.asList(
                RuneStatType.CLICK_DAMAGE_MULTIPLIER,
                RuneStatType.POISON_DAMAGE_MULTIPLIER,
                RuneStatType.BURNING_DAMAGE_MULTIPLIER,
                RuneStatType.BLEEDING_DAMAGE_MULTIPLIER,
                RuneStatType.CURSE_DAMAGE_MULTIPLIER,
                RuneStatType.WITHER_DAMAGE_MULTIPLIER,
                RuneStatType.DECAY_DAMAGE_MULTIPLIER
        );
        for (RuneStatType type : channelMultipliers) {
            check(type.isDamageMultiplier(), type.name() + " should be accepted as a delayed-damage channel");
        }
        check(!RuneStatType.ALL_DAMAGE_MULTIPLIER.isDamageMultiplier(), "Delayed channel damage must name a concrete channel");

        RuneCompositeEffect proc = RuneCompositeEffect.clickDamageProc(15.0D, 5.1D);
        check(proc.tooltipLines().equals(Arrays.asList(
                "15% шанс нанести",
                "×5.1 урона при клике"
        )), "Click proc tooltip must use the approved two-line layout");

        RuneCompositeEffect delayed = RuneCompositeEffect.delayedDamage(
                15.0D,
                RuneStatType.POISON_DAMAGE_MULTIPLIER,
                3.0D
        );
        check(delayed.tooltipLines().equals(Arrays.asList(
                "Через 15 сек:",
                "Урон Яда ×3"
        )), "Delayed damage tooltip should stay compact and channel-specific");

        RuneCompositeEffect temporary = RuneCompositeEffect.temporaryAllDamage(1.45D);
        check(temporary.tooltipLines().equals(Arrays.asList(
                "Клики временно увеличивают",
                "Весь урон ×1.45"
        )), "Temporary all-damage reference effect should wrap to two lines");

        RuneCompositeEffect effectiveness = RuneCompositeEffect.enhancementsMoreEffective();
        check(effectiveness.tooltipLines().equals(Arrays.asList("Усиления эффективнее")),
                "Reference-only enhancement effectiveness must remain formula-free");

        RuneTargetedPower targeted = new RuneTargetedPower("mace", "Булава", 5.6D);
        check(targeted.type() == RuneStatType.CARD_POWER_FLAT, "Targeted power must stay distinct from channel multipliers");
        check(targeted.tooltipLine().equals("+5.6 к силе рун: Булава"), "Targeted power tooltip should preserve the reference meaning");

        System.out.println("v1.2.10 rune stat types and composite reference effects regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
