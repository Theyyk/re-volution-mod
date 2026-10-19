package com.example.revolution.admin.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Issues one-time short-lived challenges and verifies HMAC-SHA-256 proofs. */
public final class AdminChallengeService implements AutoCloseable {
    public static final long CHALLENGE_TTL_MILLIS = 30_000L;
    private static final int MAX_OUTSTANDING = 8;
    private static final String PROTOCOL = "REvolutionAdmin/v1";
    private final SecureRandom random;
    private final LongSupplier clock;
    private final byte[] secret;
    private final Map<String, ChallengeState> outstanding = new LinkedHashMap<>();

    public AdminChallengeService(byte[] secret) {
        this(secret, new SecureRandom(), System::currentTimeMillis);
    }

    AdminChallengeService(byte[] secret, SecureRandom random, LongSupplier clock) {
        if (secret == null || secret.length < AdminInstallationSecretStore.SECRET_BYTES) {
            throw new IllegalArgumentException("installation secret must contain at least 256 bits");
        }
        this.secret = secret.clone();
        this.random = random;
        this.clock = clock;
    }

    public synchronized Challenge issue() {
        purgeExpired();
        if (outstanding.size() >= MAX_OUTSTANDING) {
            Iterator<String> iterator = outstanding.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        String id = randomToken(16);
        String nonce = randomToken(32);
        long expiresAt = clock.getAsLong() + CHALLENGE_TTL_MILLIS;
        Challenge challenge = new Challenge(id, nonce, expiresAt);
        outstanding.put(id, new ChallengeState(challenge));
        return challenge;
    }

    /** Consumes the challenge even when the proof is wrong, preventing proof-oracle retries. */
    public synchronized boolean verify(String challengeId, String proof) {
        if (challengeId == null || proof == null) return false;
        ChallengeState state = outstanding.remove(challengeId);
        if (state == null || state.challenge.expiresAt <= clock.getAsLong()) return false;
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(proof);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] expected = hmac(secret, canonical(state.challenge));
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            java.util.Arrays.fill(expected, (byte) 0);
            java.util.Arrays.fill(actual, (byte) 0);
        }
    }

    static String canonical(Challenge challenge) {
        return PROTOCOL + "\n" + challenge.id + "\n" + challenge.nonce + "\n" + challenge.expiresAt;
    }

    static byte[] hmac(byte[] secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", e);
        }
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        } finally {
            java.util.Arrays.fill(value, (byte) 0);
        }
    }

    private void purgeExpired() {
        long now = clock.getAsLong();
        for (String id : new ArrayList<>(outstanding.keySet())) {
            ChallengeState state = outstanding.get(id);
            if (state != null && state.challenge.expiresAt <= now) outstanding.remove(id);
        }
    }

    @Override public synchronized void close() {
        outstanding.clear();
        java.util.Arrays.fill(secret, (byte) 0);
    }

    public static final class Challenge {
        public final String id;
        public final String nonce;
        public final long expiresAt;

        Challenge(String id, String nonce, long expiresAt) {
            this.id = id;
            this.nonce = nonce;
            this.expiresAt = expiresAt;
        }
    }

    private static final class ChallengeState {
        final Challenge challenge;
        ChallengeState(Challenge challenge) { this.challenge = challenge; }
    }
}
