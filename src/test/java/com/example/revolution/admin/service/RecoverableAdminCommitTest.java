package com.example.revolution.admin.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecoverableAdminCommitTest {
    static class Port implements RecoverableAdminCommit.Port {
        String state = "BEFORE", journal = "NONE"; String fail;
        public void pending() { if ("pending".equals(fail)) throw new IllegalStateException(); journal = "PENDING"; }
        public void after() { assertEquals("PENDING", journal); state = "PARTIAL"; if ("after".equals(fail) || "rollback".equals(fail)) throw new IllegalStateException(); state = "AFTER"; }
        public void before() { if ("rollback".equals(fail)) throw new IllegalStateException(); state = "BEFORE"; }
        public void committed() { if ("marker".equals(fail)) throw new IllegalStateException(); journal = "COMMITTED"; }
        public void reverted() { journal = "REVERTED"; }
    }
    @Test void durablePendingPrecedesWrites() { Port port = new Port(); RecoverableAdminCommit.execute(port); assertEquals("AFTER", port.state); assertEquals("COMMITTED", port.journal); }
    @Test void journalFailureDoesNotWriteGameplayState() { Port port = new Port(); port.fail = "pending"; assertThrows(RuntimeException.class, () -> RecoverableAdminCommit.execute(port)); assertEquals("BEFORE", port.state); }
    @Test void midwayFailureRestoresWholeBeforeImage() { Port port = new Port(); port.fail = "after"; assertThrows(RuntimeException.class, () -> RecoverableAdminCommit.execute(port)); assertEquals("BEFORE", port.state); assertEquals("REVERTED", port.journal); }
    @Test void failedRollbackRetainsPendingForRestartRecovery() { Port port = new Port(); port.fail = "rollback"; assertThrows(RuntimeException.class, () -> RecoverableAdminCommit.execute(port)); assertEquals("PENDING", port.journal); port.fail = null; port.before(); port.reverted(); assertEquals("BEFORE", port.state); }
    @Test void unknownFinalMarkerRetainsCompleteAfterAndPendingDecision() { Port port = new Port(); port.fail = "marker"; assertThrows(RuntimeException.class, () -> RecoverableAdminCommit.execute(port)); assertEquals("AFTER", port.state); assertEquals("PENDING", port.journal); port.before(); port.reverted(); assertEquals("BEFORE", port.state); }
}
