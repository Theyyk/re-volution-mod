package com.example.revolution.rune;
import java.util.Arrays;
import java.util.List;

final class RuneStatContentTest {
    static void run() {
        check(RuneStatType.values().length == 30, "v1.2.10 scalar stat catalog should contain 30 reference-backed types");

        check(RuneStatType.CLICK_DAMAGE_FLAT.format(12.5D).equals("+12.5"), "Flat click damage formatting changed");
        check(RuneStatType.POISON_DAMAGE_MULTIPLIER.format(3.0D).equals("x3"), "Damage multiplier formatting changed");
        check(RuneStatType.POISON_VULNERABILITY.format(25.0D).equals("+25%"), "Vulnerability should be percent-based");
        check(RuneStatType.CLICK_CRIT_CHANCE.format(9.0D).equals("+9%"), "Crit chance should be percent-based");
        check(RuneStatType.CLICK_CRIT_DAMAGE_MULTIPLIER.format(5.1D).equals("x5.1"), "Crit damage should be multiplier-based");
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
        check(proc.activation == RuneCompositeEffect.Activation.CLICK_HIT_MOB,
                "Click proc must be tied to an actual mob hit");
        check(proc.tooltipLines().equals(Arrays.asList(
                "15% шанс нанести",
                "x5.1 урона при клике"
        )), "Click proc tooltip must use the approved two-line layout");

        RuneCompositeEffect delayed = RuneCompositeEffect.delayedDamage(
                15.0D,
                RuneStatType.POISON_DAMAGE_MULTIPLIER,
                3.0D
        );
        check(delayed.tooltipLines().equals(Arrays.asList(
                "Через 15 сек:",
                "Урон Яда x3"
        )), "Delayed damage tooltip should stay compact and channel-specific");

        RuneCompositeEffect temporary = RuneCompositeEffect.temporaryAllDamage(1.45D, 5.0D, 1);
        check(temporary.maxStacks == 1, "Rage amulet temporary all-damage effect should currently allow exactly one stack");
        check(temporary.durationSeconds == 5.0D, "Rage amulet should currently use a five-second duration");
        check(temporary.activation == RuneCompositeEffect.Activation.CLICK_HIT_MOB,
                "Rage amulet must activate on a click hit against a mob, not an empty click");
        check(temporary.tooltipLines().equals(Arrays.asList(
                "Попадание кликом увеличивает",
                "Весь урон x1.45 на 5 сек"
        )), "Temporary all-damage reference effect should wrap to two lines");

        boolean rejectedZeroStacks = false;
        try {
            RuneCompositeEffect.temporaryAllDamage(1.45D, 5.0D, 0);
        } catch (IllegalArgumentException expected) {
            rejectedZeroStacks = true;
        }
        check(rejectedZeroStacks, "Temporary all-damage effect must reject a zero stack limit");

        RuneCompositeEffect effectiveness = RuneCompositeEffect.enhancementsMoreEffective();
        check(effectiveness.tooltipLines().equals(Arrays.asList("Усиления эффективнее")),
                "Reference-only enhancement effectiveness must remain formula-free");

        RuneTargetedPower targeted = new RuneTargetedPower("mace", "Булава", 5.6D);
        check(targeted.type() == RuneStatType.RUNE_POWER_FLAT, "Targeted power must stay distinct from channel multipliers");
        check(targeted.tooltipLine(RuneInventory.NORMAL_RANK, 1.0D).equals("+5.6 к силе Булава"),
                "Targeted power tooltip should preserve the approved compact wording");
        check(targeted.tooltipLine(RuneInventory.IMPROVED_RANK, 1.5D).equals("+25.2 к силе Булава"),
                "Targeted power should scale with the owning rune rank and enhancement");

        System.out.println("v1.2.10 rune stat types and composite reference effects regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
