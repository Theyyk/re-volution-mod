package com.example.revolution.admin.api.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Owns short-lived in-memory bearer sessions. Tokens are invalidated on shutdown. */
public final class AdminSessionService implements AutoCloseable {
    public static final long IDLE_TTL_MILLIS = 5 * 60_000L;
    public static final long ABSOLUTE_TTL_MILLIS = 15 * 60_000L;
    private static final int MAX_SESSIONS = 4;
    private final SecureRandom random;
    private final LongSupplier clock;
    private final Map<String, Session> sessions = new HashMap<>();

    public AdminSessionService() {
        this(new SecureRandom(), System::currentTimeMillis);
    }

    AdminSessionService(SecureRandom random, LongSupplier clock) {
        this.random = random;
        this.clock = clock;
    }

    public synchronized String issueAllCapabilities() {
        purgeExpired();
        if (sessions.size() >= MAX_SESSIONS) {
            String oldest = null;
            long oldestCreated = Long.MAX_VALUE;
            for (Map.Entry<String, Session> entry : sessions.entrySet()) {
                if (entry.getValue().createdAt < oldestCreated) {
                    oldestCreated = entry.getValue().createdAt;
                    oldest = entry.getKey();
                }
            }
            if (oldest != null) sessions.remove(oldest);
        }
        String token = randomToken(32);
        long now = clock.getAsLong();
        sessions.put(token, new Session(now, now, EnumSet.allOf(AdminApiCapability.class)));
        return token;
    }

    public synchronized boolean authorize(String token, AdminApiCapability capability) {
        if (token == null || capability == null) return false;
        Session session = sessions.get(token);
        if (session == null) return false;
        long now = clock.getAsLong();
        if (isExpired(session, now)) {
            sessions.remove(token);
            return false;
        }
        if (!session.capabilities.contains(capability)) return false;
        session.lastSeenAt = now;
        return true;
    }

    private void purgeExpired() {
        long now = clock.getAsLong();
        for (String token : new ArrayList<>(sessions.keySet())) {
            Session session = sessions.get(token);
            if (session != null && isExpired(session, now)) sessions.remove(token);
        }
    }

    private boolean isExpired(Session session, long now) {
        return now - session.lastSeenAt >= IDLE_TTL_MILLIS || now - session.createdAt >= ABSOLUTE_TTL_MILLIS;
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

    @Override public synchronized void close() {
        sessions.clear();
    }

    private static final class Session {
        final long createdAt;
        long lastSeenAt;
        final EnumSet<AdminApiCapability> capabilities;

        Session(long createdAt, long lastSeenAt, EnumSet<AdminApiCapability> capabilities) {
            this.createdAt = createdAt;
            this.lastSeenAt = lastSeenAt;
            this.capabilities = capabilities;
        }
    }
}
