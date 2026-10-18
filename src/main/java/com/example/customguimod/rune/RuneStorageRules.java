package com.example.customguimod.rune;
import java.util.List;

/** Storage-facing boundary for the existing rune rank and legacy-slot rules. */
public final class RuneStorageRules {
    public static final int NORMAL_RANK = RuneInventory.NORMAL_RANK;
    public static final int ADVANCED_RANK = RuneInventory.ADVANCED_RANK;

    private RuneStorageRules() {}

    public static List<Integer> freePurchaseSlots(List<Integer> occupiedSlots) {
        return RuneInventory.freePurchaseSlots(occupiedSlots);
    }
}
