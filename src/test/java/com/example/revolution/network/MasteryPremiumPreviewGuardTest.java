package com.example.revolution.network;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.prestige.MasteryTierPolicy;
import org.junit.jupiter.api.Test;

/** Premium confirmation must be tied to the previewed run, mode and Balance Core revision. */
public final class MasteryPremiumPreviewGuardTest {
    private static final MasteryTierPolicy.PremiumPreservePolicy POLICY =
            new MasteryTierPolicy.PremiumPreservePolicy(true, 100, true, true);

    @Test public void sameRunSameRevisionCanConfirmWithinLifetime() {
        MasteryActionPacketHandler.PremiumPreviewTicket ticket =
                new MasteryActionPacketHandler.PremiumPreviewTicket("run-a", "premium", "rev-a", POLICY, 1000);
        assertTrue(ticket.matches("run-a", "premium", "rev-a", POLICY, 1500));
        assertFalse(ticket.matches("run-b", "premium", "rev-a", POLICY, 1500));
        assertFalse(ticket.matches("run-a", "reset_premium", "rev-a", POLICY, 1500));
        assertFalse(ticket.matches("run-a", "premium", "rev-b", POLICY, 1500));
        assertFalse(ticket.matches("run-a", "premium", "rev-a", POLICY, 121001));
    }

    @Test public void reconfiguredCostOrPermissionsRequireNewPreview() {
        MasteryActionPacketHandler.PremiumPreviewTicket ticket =
                new MasteryActionPacketHandler.PremiumPreviewTicket("run-a", "reset_premium", "rev-a", POLICY, 0);
        assertFalse(ticket.matches("run-a", "reset_premium", "rev-a",
                new MasteryTierPolicy.PremiumPreservePolicy(true, 200, true, true), 10));
        assertFalse(ticket.matches("run-a", "reset_premium", "rev-a",
                new MasteryTierPolicy.PremiumPreservePolicy(true, 100, false, true), 10));
        assertFalse(ticket.matches("run-a", "reset_premium", "rev-a",
                new MasteryTierPolicy.PremiumPreservePolicy(false, 100, true, true), 10));
    }
}
