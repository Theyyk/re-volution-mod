package com.example.revolution.rune.service;

import com.example.revolution.rune.definition.PrimaryRuneCatalog;
import com.example.revolution.rune.definition.RuneStat;
import com.example.revolution.rune.definition.RuneStatType;
import com.example.revolution.rune.definition.RuneTargetedPower;
import com.example.revolution.rune.repository.RuneRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/** Server-side ordinary Rune Share values matching E-menu's final stat preview. */
public final class RuneShareStatFormatter {
    private RuneShareStatFormatter() {}

    /** Only same-player, same-collection owned ranks are passed to this pure resolver. */
    public static double targetedBasePower(int targetSlot, List<RuneRecord> owned,
                                            IntToDoubleFunction multiplierForSlot) {
        String targetKey = targetKey(targetSlot);
        if (targetKey.isEmpty() || owned == null || multiplierForSlot == null) return 0.0D;

        // Match the client inventory's best factual rank per rune index.
        int[] ranks = new int[PrimaryRuneCatalog.size()];
        for (RuneRecord record : owned)
            if (record != null && record.runeIndex >= 0 && record.runeIndex < ranks.length
                    && record.layer > ranks[record.runeIndex])
                ranks[record.runeIndex] = record.layer;

        double total = 0.0D;
        for (int slot = 0; slot < ranks.length; slot++) {
            if (ranks[slot] <= 0) continue;
            PrimaryRuneCatalog.Definition definition = PrimaryRuneCatalog.at(slot);
            if (definition == null) continue;
            double enhancementMultiplier = multiplierForSlot.applyAsDouble(slot);
            for (RuneTargetedPower power : definition.targetedPowers)
                if (power != null && targetKey.equals(power.targetKey))
                    total += Math.max(0.0D, power.resolve(ranks[slot], enhancementMultiplier));
        }
        return Double.isFinite(total) ? total : 0.0D;
    }

    /** Do not apply targeted bonuses to percentage, multiplier or other stat types. */
    public static List<String> finalStatLines(PrimaryRuneCatalog.Definition rune,
                                               int slot, int rank, double ownMultiplier,
                                               List<RuneRecord> owned,
                                               IntToDoubleFunction multiplierForSlot) {
        List<String> lines = new ArrayList<>();
        if (rune == null) return lines;
        double bonus = targetedBasePower(slot, owned, multiplierForSlot);
        for (RuneStat stat : rune.stats) {
            if (stat == null || stat.type == null) continue;
            boolean targetedFlat = stat.type == RuneStatType.CLICK_DAMAGE_FLAT
                    || stat.type == RuneStatType.POISON_DAMAGE_FLAT;
            double value = stat.resolve(rank, ownMultiplier, targetedFlat ? bonus : 0.0D).finalValue;
            lines.add(stat.type.displayName + " " + stat.type.format(value));
        }
        return lines;
    }

    private static String targetKey(int slot) {
        if (slot >= 0 && slot <= 3) return "killer_dagger";
        if (slot >= 13 && slot <= 16) return "snake_fang";
        return "";
    }
}
