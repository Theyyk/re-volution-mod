package com.example.revolution.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Regression: premium reset must CAS the canonical string-valued crystal wallet. */
public final class MongoPrestigePremiumWalletTest {
    @Test public void stringWalletIsDebitedExactlyAndGuardedByObservedValue() {
        Document filter = new Document("_id", "player").append("prestige.activeRun.runId", "run-1");
        Document set = new Document("prestige", new Document("lastResetRunId", "run-1"));
        MongoPrestigeRepository.appendCrystalDebit(filter, set, "125.50", 100);

        assertEquals("125.50", filter.getString("crystals"));
        assertEquals("25.5", set.getString("crystals"));
        assertEquals("run-1", filter.getString("prestige.activeRun.runId"));
        assertFalse(set.containsKey("$inc"));
    }

    @Test public void debitHandlesLargeExactDecimalBalances() {
        Document filter = new Document();
        Document set = new Document();
        MongoPrestigeRepository.appendCrystalDebit(filter, set, "900719925474099312345678", 300);
        assertEquals("900719925474099312345378", set.getString("crystals"));
    }

    @Test public void insufficientBalanceNeverBuildsDebit() {
        Document filter = new Document();
        Document set = new Document();
        assertThrows(IllegalArgumentException.class,
                () -> MongoPrestigeRepository.appendCrystalDebit(filter, set, "99", 100));
        assertFalse(filter.containsKey("crystals"));
        assertFalse(set.containsKey("crystals"));
    }
}
