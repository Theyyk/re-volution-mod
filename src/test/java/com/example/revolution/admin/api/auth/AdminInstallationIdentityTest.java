package com.example.revolution.admin.api.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AdminInstallationIdentityTest {
    @TempDir Path directory;
    @Test void freshConfigurationGetsStablePublicIdentity() throws Exception {
        Path config = directory.resolve("new/config");
        String id = AdminInstallationIdentity.initialize(config.toFile());
        assertEquals(id, UUID.fromString(id).toString());
        assertEquals(id, AdminInstallationIdentity.initialize(config.toFile()));
        assertFalse(Files.exists(config.resolve("revolution-admin.secret")));
    }
    @Test void invalidExistingIdentityIsNotSilentlyReplaced() throws Exception {
        Path identity = directory.resolve("revolution-admin.installation-id");
        Files.write(identity, "malformed".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> AdminInstallationIdentity.initialize(directory.toFile()));
        assertEquals("malformed", new String(Files.readAllBytes(identity), StandardCharsets.US_ASCII));
    }
}
