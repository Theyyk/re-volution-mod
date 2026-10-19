package com.example.revolution.admin.api.auth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Owns the local installation secret. The secret is never a repository asset or an HTTP bearer credential. */
public final class AdminInstallationSecretStore implements AutoCloseable {
    static final int SECRET_BYTES = 32;
    private static final String FILE_NAME = "revolution-admin.secret";
    private final SecureRandom random;
    private byte[] secret;

    public AdminInstallationSecretStore() {
        this(new SecureRandom());
    }

    AdminInstallationSecretStore(SecureRandom random) {
        this.random = random;
    }

    public synchronized byte[] initialize(File configDirectory) throws IOException {
        if (secret != null) return secret.clone();
        if (configDirectory == null) throw new IOException("Admin API config directory is unavailable");
        Path directory = configDirectory.toPath();
        Files.createDirectories(directory);
        Path path = directory.resolve(FILE_NAME);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) createSecret(path);
        requireRegularUnlinkedFile(path);
        byte[] loaded = decode(Files.readAllBytes(path));
        if (loaded.length != SECRET_BYTES) throw new IOException("Admin API installation secret is invalid");
        hardenPermissions(path);
        secret = loaded;
        return secret.clone();
    }

    private void createSecret(Path path) throws IOException {
        byte[] generated = new byte[SECRET_BYTES];
        random.nextBytes(generated);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(generated) + "\n";
        try {
            Files.createFile(path);
            requireRegularUnlinkedFile(path);
            hardenPermissions(path);
            try (FileOutputStream out = new FileOutputStream(path.toFile(), false)) {
                out.write(encoded.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                out.getFD().sync();
            }
            requireRegularUnlinkedFile(path);
            hardenPermissions(path);
        } catch (IOException e) {
            java.util.Arrays.fill(generated, (byte) 0);
            throw e;
        }
        java.util.Arrays.fill(generated, (byte) 0);
    }

    private static void requireRegularUnlinkedFile(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Admin API installation secret path is not a regular file");
        }
    }

    private static byte[] decode(byte[] bytes) throws IOException {
        String encoded = new String(bytes, StandardCharsets.US_ASCII).trim();
        try {
            return Base64.getUrlDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IOException("Admin API installation secret is invalid", e);
        }
    }

    private static void hardenPermissions(Path path) throws IOException {
        requireRegularUnlinkedFile(path);
        boolean hardened = false;
        try {
            Set<PosixFilePermission> ownerOnly = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, ownerOnly);
            hardened = true;
        } catch (UnsupportedOperationException ignored) {
            // Windows and other non-POSIX filesystems are handled below.
        }

        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl != null) {
            AclEntry owner = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path, LinkOption.NOFOLLOW_LINKS))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            acl.setAcl(Collections.singletonList(owner));
            hardened = true;
        }

        if (!hardened) {
            File file = path.toFile();
            boolean changed = file.setReadable(false, false)
                    && file.setWritable(false, false)
                    && file.setExecutable(false, false)
                    && file.setReadable(true, true)
                    && file.setWritable(true, true);
            if (!changed) throw new IOException("Unable to restrict Admin API installation secret permissions");
        }
    }

    @Override public synchronized void close() {
        if (secret != null) java.util.Arrays.fill(secret, (byte) 0);
        secret = null;
    }
}
