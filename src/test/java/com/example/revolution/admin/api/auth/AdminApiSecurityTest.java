package com.example.revolution.admin.api.auth;

import com.example.revolution.admin.api.AdminApiBindPolicy;
import java.io.File;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AdminApiSecurityTest {
    public static void main(String[] args) throws Exception {
        testBindPolicy();
        testInstallationSecretStability();
        testChallengeReplayAndExpiry();
        testSessionExpiryAndShutdown();
        testAuthRateLimit();
        System.out.println("Admin API loopback, secret, HMAC replay/expiry, session lifecycle and auth rate limits passed");
    }

    private static void testBindPolicy() throws Exception {
        require(AdminApiBindPolicy.isAllowed(InetAddress.getByName("127.0.0.1")), "IPv4 loopback must be allowed");
        require(!AdminApiBindPolicy.isAllowed(InetAddress.getByName("0.0.0.0")), "wildcard bind must be rejected");
        require(!AdminApiBindPolicy.isAllowed(InetAddress.getByName("192.0.2.1")), "remote IPv4 must be rejected");
        require(!AdminApiBindPolicy.isAllowed(InetAddress.getByName("::1")), "v1.3.2 must bind exactly to 127.0.0.1");
    }

    private static void testInstallationSecretStability() throws Exception {
        File directory = Files.createTempDirectory("revolution-admin-secret-").toFile();
        try {
            byte[] first;
            try (AdminInstallationSecretStore store = new AdminInstallationSecretStore()) {
                first = store.initialize(directory);
                require(first.length == 32, "installation secret must be 256 bits");
            }
            byte[] second;
            try (AdminInstallationSecretStore store = new AdminInstallationSecretStore()) {
                second = store.initialize(directory);
            }
            require(java.security.MessageDigest.isEqual(first, second), "installation secret must survive restart");
            java.util.Arrays.fill(first, (byte) 0);
            java.util.Arrays.fill(second, (byte) 0);
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void testChallengeReplayAndExpiry() {
        byte[] secret = secret();
        long[] now = {1_000L};
        LongSupplier clock = () -> now[0];
        AdminChallengeService service = new AdminChallengeService(secret, new SecureRandom(), clock);
        try {
            AdminChallengeService.Challenge challenge = service.issue();
            String proof = proof(secret, challenge);
            require(service.verify(challenge.id, proof), "valid HMAC proof must authenticate");
            require(!service.verify(challenge.id, proof), "challenge replay must fail");

            AdminChallengeService.Challenge expired = service.issue();
            String expiredProof = proof(secret, expired);
            now[0] = expired.expiresAt;
            require(!service.verify(expired.id, expiredProof), "expired challenge must fail");
        } finally {
            service.close();
            java.util.Arrays.fill(secret, (byte) 0);
        }
    }

    private static void testSessionExpiryAndShutdown() {
        long[] now = {10_000L};
        LongSupplier clock = () -> now[0];
        AdminSessionService sessions = new AdminSessionService(new SecureRandom(), clock);
        String token = sessions.issueAllCapabilities();
        require(sessions.authorize(token, AdminApiCapability.SERVER_READ), "fresh session must authorize its capability");
        now[0] += AdminSessionService.IDLE_TTL_MILLIS;
        require(!sessions.authorize(token, AdminApiCapability.SERVER_READ), "idle session must expire");
        String shutdownToken = sessions.issueAllCapabilities();
        sessions.close();
        require(!sessions.authorize(shutdownToken, AdminApiCapability.SERVER_READ), "shutdown must invalidate all sessions");
    }

    private static void testAuthRateLimit() {
        byte[] secret = secret();
        long[] now = {50_000L};
        AdminApiAuthService auth = new AdminApiAuthService(secret, new SecureRandom(), () -> now[0]);
        try {
            for (int i = 0; i < 30; i++) require(auth.issueChallenge() != null, "challenge within rate limit rejected");
            require(auth.issueChallenge() == null, "challenge rate limit must fail closed");
            now[0] += 60_000L;
            require(auth.issueChallenge() != null, "rate limit must recover after its window");
        } finally {
            auth.close();
            java.util.Arrays.fill(secret, (byte) 0);
        }
    }

    private static String proof(byte[] secret, AdminChallengeService.Challenge challenge) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] bytes = mac.doFinal(AdminChallengeService.canonical(challenge).getBytes(StandardCharsets.UTF_8));
            try {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            } finally {
                java.util.Arrays.fill(bytes, (byte) 0);
            }
        } catch (GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] secret() {
        byte[] secret = new byte[32];
        for (int i = 0; i < secret.length; i++) secret[i] = (byte) (i + 1);
        return secret;
    }

    private static void deleteRecursively(File file) throws Exception {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        Files.deleteIfExists(file.toPath());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
