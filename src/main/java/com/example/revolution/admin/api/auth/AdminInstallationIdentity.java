package com.example.revolution.admin.api.auth;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/** Public installation identity is distinct from the private authentication secret. */
public final class AdminInstallationIdentity {
    private AdminInstallationIdentity() {}
    public static String initialize(File directory) throws IOException {
        Files.createDirectories(directory.toPath());
        Path path = directory.toPath().resolve("revolution-admin.installation-id");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
            Files.write(path, UUID.randomUUID().toString().getBytes(StandardCharsets.US_ASCII), StandardOpenOption.CREATE_NEW);
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 64)
            throw new IOException("Invalid Admin installation identity file");
        try { return UUID.fromString(new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim()).toString(); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid Admin installation identity", e); }
    }
}
