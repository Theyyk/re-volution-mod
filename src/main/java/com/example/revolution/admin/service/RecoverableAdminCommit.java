package com.example.revolution.admin.service;

/** Write-ahead recovery protocol, independently fault-testable without Minecraft/Mongo. */
public final class RecoverableAdminCommit {
    public interface Port {
        void pending();
        void after();
        void before();
        void committed();
        void reverted();
    }
    private RecoverableAdminCommit() {}
    public static void execute(Port port) {
        port.pending(); // Must be durably acknowledged before the first gameplay configuration write.
        try { port.after(); }
        catch (RuntimeException failure) {
            try { port.before(); port.reverted(); }
            catch (RuntimeException recovery) { failure.addSuppressed(recovery); }
            throw failure;
        }
        // Marker acknowledgement can be ambiguous; never attempt a partial undo after all writes completed.
        // A still-PENDING journal restores the complete before-image on the next recovery.
        port.committed();
    }
}
