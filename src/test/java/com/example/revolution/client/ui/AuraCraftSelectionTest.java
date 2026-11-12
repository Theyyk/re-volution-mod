package com.example.revolution.client.ui;

import com.example.revolution.network.AuraSnapshotPacket;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuraCraftSelectionTest {
    private static AuraSnapshotPacket.Entry e(String id, String rarity, boolean pin, boolean active) {
        return new AuraSnapshotPacket.Entry(id, rarity, "CHEST", 0L, "", "", pin, active);
    }
    @Test void userMustChooseExactlyDistinctEligibleInstances() {
        java.util.List<AuraSnapshotPacket.Entry> list = Arrays.asList(
                e("a", "COMMON", false, false), e("b", "COMMON", false, false),
                e("c", "COMMON", false, false), e("p", "COMMON", true, false),
                e("on", "COMMON", false, true), e("other", "RARE", false, false));
        assertTrue(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("c","a","b"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","a","b"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","b","p"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","b","on"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","b","other"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","b","missing"),3));
        assertFalse(AuraCraftSelection.valid(list,"COMMON",Arrays.asList("a","b"),3));
    }
}
