package com.example.revolution.rune.state;

import com.example.revolution.rune.definition.RuneDamageChannel;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuneCombatStateTest {
    @Test
    void globalAndChannelTemporaryEffectsExpireIndependently() {
        UUID player = UUID.randomUUID();
        RuneCombatState state = new RuneCombatState();

        state.activate(player, 120L);
        state.activate(player, RuneDamageChannel.BURNING, 140L);
        state.activate(player, RuneDamageChannel.CURSE, 110L);

        assertTrue(state.isActive(player, 119L));
        assertFalse(state.isActive(player, 120L));
        assertTrue(state.isActive(player, RuneDamageChannel.BURNING, 120L));
        assertFalse(state.isActive(player, RuneDamageChannel.CURSE, 120L));
        assertFalse(state.isActive(player, RuneDamageChannel.CLICK, 120L));
    }

    @Test
    void refreshNeverShortensExistingExpiryAndClearDropsEverything() {
        UUID player = UUID.randomUUID();
        RuneCombatState state = new RuneCombatState();

        state.activate(player, 200L);
        state.activate(player, 150L);
        state.activate(player, RuneDamageChannel.BLEEDING, 220L);
        state.activate(player, RuneDamageChannel.BLEEDING, 180L);
        assertTrue(state.isActive(player, 199L));
        assertTrue(state.isActive(player, RuneDamageChannel.BLEEDING, 219L));

        state.clear();
        assertFalse(state.isActive(player, 1L));
        assertFalse(state.isActive(player, RuneDamageChannel.BLEEDING, 1L));
    }
}
