package com.example.revolution.rune.definition;

/**
 * Compatibility vocabulary for the source/reference rune themes.
 * Gameplay code must use RuneDamageChannel; these names exist only to make the agreed content
 * mapping explicit and auditable while collections are migrated to native RE:volution names.
 */
public enum RuneReferenceTheme {
    CLICK(RuneDamageChannel.CLICK),
    EARTH(RuneDamageChannel.POISON),
    ICE(RuneDamageChannel.DECAY),
    SUN(RuneDamageChannel.WITHER),
    ELECTRIC(RuneDamageChannel.BLEEDING),
    FIRE(RuneDamageChannel.BURNING),
    MOON(RuneDamageChannel.CURSE);

    private final RuneDamageChannel channel;

    RuneReferenceTheme(RuneDamageChannel channel) {
        this.channel = channel;
    }

    public RuneDamageChannel channel() {
        return channel;
    }
}
