package com.example.revolution.admin.api;

import com.example.revolution.RevolutionMod;
import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.api.auth.AdminChallengeService;
import com.example.revolution.admin.api.auth.AdminInstallationSecretStore;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;

/**
 * Loopback-only HTTP adapter for the private RE:volution Admin application.
 * Business operations do not belong here; routes authenticate/authorize and delegate to typed services.
 */
public final class AdminApiServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 18765;
    private static final String CHALLENGE_PATH = "/api/v1/auth/challenge";
    private static final String SESSION_PATH = "/api/v1/auth/session";
    private static final String STATUS_PATH = "/api/v1/status";
    private final Logger logger;
    private final AdminInstallationSecretStore secretStore = new AdminInstallationSecretStore();
    private HttpServer server;
    private ThreadPoolExecutor executor;
    private AdminApiAuthService auth;

    public AdminApiServer(Logger logger) {
        this.logger = logger;
    }

    public synchronized void start(File configDirectory) throws IOException {
        if (server != null) return;
        InetAddress bindAddress = AdminApiBindPolicy.loopback();
        AdminApiBindPolicy.requireAllowed(bindAddress);
        int port = resolvePort();
        byte[] installationSecret = secretStore.initialize(configDirectory);
        AdminApiAuthService newAuth = new AdminApiAuthService(installationSecret);
        java.util.Arrays.fill(installationSecret, (byte) 0);

        HttpServer newServer = null;
        ThreadPoolExecutor newExecutor = null;
        try {
            newServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 16);
            newExecutor = new ThreadPoolExecutor(
                    1,
                    2,
                    30L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(16),
                    new AdminApiThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());
            newServer.setExecutor(newExecutor);
            auth = newAuth;
            newServer.createContext(CHALLENGE_PATH, exact(CHALLENGE_PATH, this::handleChallenge));
            newServer.createContext(SESSION_PATH, exact(SESSION_PATH, this::handleSession));
            newServer.createContext(STATUS_PATH, exact(STATUS_PATH, this::handleStatus));
            newServer.start();
            server = newServer;
            executor = newExecutor;
            logger.info("RE:volution Admin API listening on 127.0.0.1:" + port);
        } catch (RuntimeException | IOException e) {
            if (newServer != null) newServer.stop(0);
            if (newExecutor != null) newExecutor.shutdownNow();
            newAuth.close();
            auth = null;
            throw e;
        }
    }

    private HttpHandler exact(String path, HttpHandler delegate) {
        return exchange -> {
            try {
                if (!path.equals(exchange.getRequestURI().getPath())) {
                    respond(exchange, 404, "{\"error\":\"not_found\"}");
                    return;
                }
                InetAddress remote = exchange.getRemoteAddress() == null ? null : exchange.getRemoteAddress().getAddress();
                if (!AdminApiBindPolicy.isAllowed(remote)) {
                    respond(exchange, 403, "{\"error\":\"forbidden\"}");
                    return;
                }
                if (isBrowserOriginRequest(exchange.getRequestHeaders())) {
                    respond(exchange, 403, "{\"error\":\"forbidden\"}");
                    return;
                }
                delegate.handle(exchange);
            } catch (RuntimeException e) {
                logger.warn("Admin API request failed without exposing request credentials", e);
                safeInternalError(exchange);
            } finally {
                exchange.close();
            }
        };
    }

    private void handleChallenge(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST") || !requireEmptyBody(exchange)) return;
        AdminChallengeService.Challenge challenge = auth.issueChallenge();
        if (challenge == null) {
            respond(exchange, 429, "{\"error\":\"rate_limited\"}");
            return;
        }
        respond(exchange, 200, "{\"protocol\":\"REvolutionAdmin/v1\",\"challengeId\":\""
                + challenge.id + "\",\"nonce\":\"" + challenge.nonce + "\",\"expiresAt\":" + challenge.expiresAt + "}");
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST") || !requireEmptyBody(exchange)) return;
        String challengeId = boundedHeader(exchange, "X-REvolution-Challenge", 64);
        String proof = boundedHeader(exchange, "X-REvolution-Proof", 128);
        if (challengeId == null || proof == null) {
            respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        String token = auth.openSession(challengeId, proof);
        if (token == null) {
            respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        respond(exchange, 200, "{\"token\":\"" + token + "\",\"idleTtlMillis\":300000,\"absoluteTtlMillis\":900000}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET") || !requireEmptyBody(exchange)) return;
        String authorization = boundedHeader(exchange, "Authorization", 128);
        if (!auth.authorizeBearer(authorization, AdminApiCapability.SERVER_READ)) {
            respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        respond(exchange, 200, "{\"apiVersion\":1,\"modVersion\":\"" + RevolutionMod.VERSION + "\",\"remoteMode\":false}");
    }

    private static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equals(exchange.getRequestMethod())) return true;
        exchange.getResponseHeaders().set("Allow", method);
        respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
        return false;
    }

    private static boolean requireEmptyBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        String transferEncoding = exchange.getRequestHeaders().getFirst("Transfer-Encoding");
        if (transferEncoding != null) {
            respond(exchange, 413, "{\"error\":\"body_not_allowed\"}");
            return false;
        }
        if (contentLength != null) {
            try {
                if (Long.parseLong(contentLength) != 0L) {
                    respond(exchange, 413, "{\"error\":\"body_not_allowed\"}");
                    return false;
                }
            } catch (NumberFormatException e) {
                respond(exchange, 400, "{\"error\":\"bad_request\"}");
                return false;
            }
        }
        return true;
    }

    private static boolean isBrowserOriginRequest(Headers headers) {
        return headers.getFirst("Origin") != null
                || headers.getFirst("Access-Control-Request-Method") != null
                || headers.getFirst("Sec-Fetch-Site") != null;
    }

    private static String boundedHeader(HttpExchange exchange, String name, int maxLength) {
        String value = exchange.getRequestHeaders().getFirst(name);
        if (value == null || value.length() > maxLength) return null;
        return value;
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void safeInternalError(HttpExchange exchange) {
        try {
            respond(exchange, 500, "{\"error\":\"internal_error\"}");
        } catch (IOException ignored) {
            // Connection already failed; no sensitive detail is returned.
        }
    }

    static int resolvePort() {
        String configured = System.getProperty("revolution.admin.port", Integer.toString(DEFAULT_PORT)).trim();
        try {
            int port = Integer.parseInt(configured);
            if (port < 1024 || port > 65535) throw new IllegalArgumentException("Admin API port must be between 1024 and 65535");
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Admin API port is invalid");
        }
    }

    @Override public synchronized void close() {
        HttpServer stopping = server;
        server = null;
        if (stopping != null) stopping.stop(0);
        ThreadPoolExecutor stoppingExecutor = executor;
        executor = null;
        if (stoppingExecutor != null) stoppingExecutor.shutdownNow();
        AdminApiAuthService stoppingAuth = auth;
        auth = null;
        if (stoppingAuth != null) stoppingAuth.close();
        secretStore.close();
    }

    private static final class AdminApiThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();
        @Override public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "revolution-admin-api-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, error) -> {
                // HttpServer owns request reporting; never print request data from this boundary.
            });
            return thread;
        }
    }
}
