package com.example.revolution.network;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShareChatDeliveryTest {
    @Test public void duplicateRequestsAreLimitedWithoutBlockingNormalUse() {
        UUID player = UUID.randomUUID();
        assertTrue(ShareChatDelivery.accept(player, 10000L));
        assertFalse(ShareChatDelivery.accept(player, 10000L));
        assertFalse(ShareChatDelivery.accept(player, 10849L));
        assertTrue(ShareChatDelivery.accept(player, 10850L));
    }

    @Test public void independentPlayersAndInvalidIds() {
        assertFalse(ShareChatDelivery.accept(null, 0L));
        assertTrue(ShareChatDelivery.accept(UUID.randomUUID(), 10000L));
        assertTrue(ShareChatDelivery.accept(UUID.randomUUID(), 10000L));
    }
}
