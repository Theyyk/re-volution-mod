package com.example.revolution.admin.api.auth;

import java.security.SecureRandom;
import java.util.function.LongSupplier;

/** Authentication boundary: challenge-response first, short-lived bearer session afterwards. */
public final class AdminApiAuthService implements AutoCloseable {
    private final AdminChallengeService challenges;
    private final AdminSessionService sessions;
    private final AdminAuthRateLimiter challengeLimiter;
    private final AdminAuthRateLimiter verifyLimiter;

    public AdminApiAuthService(byte[] secret) {
        this(secret, new SecureRandom(), System::currentTimeMillis);
    }

    AdminApiAuthService(byte[] secret, SecureRandom random, LongSupplier clock) {
        challenges = new AdminChallengeService(secret, random, clock);
        sessions = new AdminSessionService(random, clock);
        challengeLimiter = new AdminAuthRateLimiter(30, 60_000L, clock);
        verifyLimiter = new AdminAuthRateLimiter(20, 60_000L, clock);
    }

    public synchronized AdminChallengeService.Challenge issueChallenge() {
        if (!challengeLimiter.tryAcquire()) return null;
        return challenges.issue();
    }

    public synchronized String openSession(String challengeId, String proof) {
        if (!verifyLimiter.tryAcquire()) return null;
        if (!challenges.verify(challengeId, proof)) return null;
        return sessions.issueAllCapabilities();
    }

    public boolean authorizeBearer(String authorizationHeader, AdminApiCapability capability) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return false;
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) return false;
        return sessions.authorize(token, capability);
    }

    @Override public synchronized void close() {
        challenges.close();
        sessions.close();
    }
}
