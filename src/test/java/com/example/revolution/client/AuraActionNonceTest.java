package com.example.revolution.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.network.AuraRequestLimiter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AuraActionNonceTest {
    @Test void sharedSourceAcceptsInterleavedInventoryAndCraftRequests() {
        UUID player = UUID.randomUUID();
        try {
            // Both GUI screens call the SAME generator. Model an inventory
            // mutation, craft mutation, then a subsequent inventory mutation.
            long inventory = AuraActionNonce.next();
            long craft = AuraActionNonce.next();
            long subsequentInventory = AuraActionNonce.next();

            assertTrue(inventory < craft);
            assertTrue(craft < subsequentInventory);
            assertTrue(AuraRequestLimiter.acceptMutation(player, inventory));
            assertTrue(AuraRequestLimiter.acceptMutation(player, craft));
            assertTrue(AuraRequestLimiter.acceptMutation(player, subsequentInventory));
        } finally {
            AuraRequestLimiter.forget(player);
        }
    }

    @Test void replayedAndOlderNoncesRemainRejected() {
        UUID player = UUID.randomUUID();
        try {
            long earlier = AuraActionNonce.next();
            long later = AuraActionNonce.next();
            assertTrue(AuraRequestLimiter.acceptMutation(player, later));
            org.junit.jupiter.api.Assertions.assertFalse(AuraRequestLimiter.acceptMutation(player, earlier));
            org.junit.jupiter.api.Assertions.assertFalse(AuraRequestLimiter.acceptMutation(player, later));
        } finally {
            AuraRequestLimiter.forget(player);
        }
    }
}
