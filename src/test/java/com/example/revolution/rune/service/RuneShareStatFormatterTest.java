package com.example.revolution.rune.service;

import com.example.revolution.rune.definition.PrimaryRuneCatalog;
import com.example.revolution.rune.repository.RuneRecord;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** UI-02-D-R12: Share must expose the same targeted flat stat as the E-menu. */
public final class RuneShareStatFormatterTest {
    private static RuneRecord owned(int index, int rank) {
        return new RuneRecord(index, index, rank, 0);
    }

    private static String effect(int target, int rank, double ownMultiplier,
                                 List<RuneRecord> runes, IntToDoubleFunction multipliers) {
        List<String> lines = RuneShareStatFormatter.finalStatLines(
                PrimaryRuneCatalog.at(target), target, rank, ownMultiplier, runes, multipliers);
        assertEquals(1, lines.size());
        return lines.get(0);
    }

    @Test void snakeFangPlusZeroAndPlusOneIncludeOtherRunesTargetedPower() {
        // Two factual toxin boosters (+1 each) at one enhancement step (+10%) yield
        // +2.2 targeted power; 1 + 2.2 = 3.2; target enhancement +1 => 3.52.
        List<RuneRecord> owned = Arrays.asList(owned(13, 1), owned(20, 1), owned(21, 1));
        IntToDoubleFunction multiplier = slot -> slot == 20 || slot == 21 ? 1.1D : 1.0D;
        assertEquals("Урон Яда +3.2", effect(13, 1, 1.0D, owned, multiplier));
        assertEquals("Урон Яда +3.52", effect(13, 1, 1.1D, owned, multiplier));
    }

    @Test void unboostedTargetAndWrongTargetDoNotReceiveBonuses() {
        assertEquals("Урон Яда +1", effect(13, 1, 1.0D,
                Collections.singletonList(owned(13, 1)), slot -> 1.0D));
        assertEquals("Урон Яда +1.1", effect(13, 1, 1.1D,
                Collections.singletonList(owned(13, 1)), slot -> 1.0D));
        List<RuneRecord> otherFamily = Arrays.asList(owned(13, 1), owned(8, 1), owned(9, 1));
        assertEquals("Урон Яда +1", effect(13, 1, 1.0D, otherFamily, slot -> 1.0D));
        assertEquals("Урон Клика +3", effect(0, 1, 1.0D,
                Arrays.asList(owned(0, 1), owned(8, 1), owned(9, 1)), slot -> 1.0D));
    }

    @Test void statMultipliersDoNotAcquireTargetedFlatPower() {
        List<RuneRecord> runes = Arrays.asList(owned(11, 1), owned(20, 1), owned(21, 1));
        List<String> lines = RuneShareStatFormatter.finalStatLines(
                PrimaryRuneCatalog.at(11), 11, 1, 1.0D, runes, slot -> 1.0D);
        assertEquals(Arrays.asList("Урон Клика x1", "Урон Яда x1"), lines);
    }

    @Test void strongerFactualRankWinsAndDuplicateSourceIsNotCountedTwice() {
        List<RuneRecord> runes = Arrays.asList(owned(13, 1), owned(20, 1),
                owned(20, 2), owned(20, 1));
        assertEquals("Урон Яда +4", effect(13, 1, 1.0D, runes, slot -> 1.0D));
    }

    @Test void foreignPlayerOrOtherCollectionCannotLeakIntoTargetedShare() {
        // The resolver has no global state and uses only the owned collection
        // argument passed from the server's authenticated player UUID.
        List<RuneRecord> first = Arrays.asList(owned(13, 1), owned(20, 1));
        List<RuneRecord> second = Collections.singletonList(owned(13, 1));
        assertEquals("Урон Яда +2", effect(13, 1, 1.0D, first, slot -> 1.0D));
        assertEquals("Урон Яда +1", effect(13, 1, 1.0D, second, slot -> 1.0D));
    }
}
