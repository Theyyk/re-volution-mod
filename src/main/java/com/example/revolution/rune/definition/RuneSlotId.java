package com.example.revolution.rune.definition;

/** Stable 1..26 rune slot identity independent from GUI coordinates. */
public final class RuneSlotId {
    private final int ordinal;

    private RuneSlotId(int ordinal) {
        this.ordinal = ordinal;
    }

    public static RuneSlotId ofOrdinal(int ordinal) {
        if (ordinal < 1 || ordinal > RuneLayoutDefinition.SLOTS_PER_COLLECTION) {
            throw new IllegalArgumentException("rune slot ordinal must be 1.." + RuneLayoutDefinition.SLOTS_PER_COLLECTION);
        }
        return new RuneSlotId(ordinal);
    }

    public static RuneSlotId fromLegacyIndex(int index) {
        return ofOrdinal(index + 1);
    }

    public int ordinal() {
        return ordinal;
    }

    public int legacyIndex() {
        return ordinal - 1;
    }

    public String id() {
        return String.format(java.util.Locale.ROOT, "slot_%02d", ordinal);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RuneSlotId && ((RuneSlotId) other).ordinal == ordinal;
    }

    @Override
    public int hashCode() {
        return ordinal;
    }

    @Override
    public String toString() {
        return id();
    }
}
