package com.example.revolution.admin.api;

import com.example.revolution.RevolutionMod;
import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.api.auth.AdminChallengeService;
import com.example.revolution.admin.api.auth.AdminInstallationIdentity;
import com.example.revolution.admin.api.auth.AdminInstallationSecretStore;
import com.example.revolution.admin.service.AdminBalanceBatchService;
import com.example.revolution.admin.service.AdminMechanicsService;
import com.example.revolution.admin.service.AdminOperationsService;
import com.example.revolution.admin.service.AdminQueryService;
import com.example.revolution.admin.service.AdminStageBatchService;
import com.example.revolution.admin.service.AdminToolsService;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** Loopback-only lifecycle/auth bootstrap for the private RE:volution Admin HTTP adapter. */
public final class AdminApiServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 18765;
    private static final String CHALLENGE_PATH = "/api/v1/auth/challenge";
    private static final String SESSION_PATH = "/api/v1/auth/session";
    private static final String STATUS_PATH = "/api/v1/status";

    private final Logger logger;
    private final AdminOperationsService operations;
    private final AdminQueryService queries;
    private final AdminInstallationSecretStore secretStore = new AdminInstallationSecretStore();
    private HttpServer server;
    private ThreadPoolExecutor executor;
    private AdminApiAuthService auth;
    private AdminToolsService tools;
    private AdminStageBatchService batch;
    private BalanceService balance;
    private AdminBalanceBatchService balanceBatch;
    private String installationId;

    public void setTools(AdminToolsService tools, AdminStageBatchService batch) { this.tools = tools; this.batch = batch; }
    public void setBalance(BalanceService balance, AdminBalanceBatchService batch) { this.balance = balance; this.balanceBatch = batch; }

    public AdminApiServer(Logger logger, AdminOperationsService operations, AdminQueryService queries) {
        if (logger == null || operations == null || queries == null) throw new IllegalArgumentException("Admin API dependencies are required");
        this.logger = logger;
        this.operations = operations;
        this.queries = queries;
    }

    public synchronized void start(MinecraftServer minecraftServer, File configDirectory) throws IOException {
        if (server != null) return;
        if (minecraftServer == null) throw new IOException("MinecraftServer is unavailable for Admin API");
        InetAddress bindAddress = AdminApiBindPolicy.loopback();
        AdminApiBindPolicy.requireAllowed(bindAddress);
        int port = resolvePort();
        installationId = AdminInstallationIdentity.initialize(configDirectory);
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
            new AdminApiRoutes(logger, newAuth, operations, queries, minecraftServer).register(newServer, this::exact);
            if (tools != null) new AdminToolsRoutes(tools, batch, newAuth, minecraftServer, logger, operations).register(newServer, this::exact);
            if (balance != null && balanceBatch != null) {
                new AdminBalanceRoutes(balance, balanceBatch, newAuth, minecraftServer, logger).register(newServer, this::exact);
            }
            ServerServices services = ServerLifecycle.services();
            AdminMechanicsService mechanics = new AdminMechanicsService(
                    services.prestige, services.pets, services.gods, services.auras, services.achievements, services.uniqueRunes);
            new AdminMechanicsRoutes(mechanics, newAuth, minecraftServer).register(newServer, this::exact);
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
                    AdminApiHttp.respond(exchange, 404, "{\"error\":\"not_found\"}");
                    return;
                }
                InetAddress remote = exchange.getRemoteAddress() == null ? null : exchange.getRemoteAddress().getAddress();
                if (!AdminApiBindPolicy.isAllowed(remote)) {
                    AdminApiHttp.respond(exchange, 403, "{\"error\":\"forbidden\"}");
                    return;
                }
                if (AdminApiHttp.isBrowserOriginRequest(exchange.getRequestHeaders())) {
                    AdminApiHttp.respond(exchange, 403, "{\"error\":\"forbidden\"}");
                    return;
                }
                delegate.handle(exchange);
            } catch (RuntimeException e) {
                logger.warn("Admin API request failed without exposing request credentials", e);
                AdminApiHttp.safeInternalError(exchange);
            } finally {
                exchange.close();
            }
        };
    }

    private void handleChallenge(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "POST") || !AdminApiHttp.requireEmptyBody(exchange)) return;
        AdminChallengeService.Challenge challenge = auth.issueChallenge();
        if (challenge == null) {
            AdminApiHttp.respond(exchange, 429, "{\"error\":\"rate_limited\"}");
            return;
        }
        AdminApiHttp.respond(exchange, 200, "{\"protocol\":\"REvolutionAdmin/v1\",\"challengeId\":\""
                + challenge.id + "\",\"nonce\":\"" + challenge.nonce + "\",\"expiresAt\":" + challenge.expiresAt + "}");
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "POST") || !AdminApiHttp.requireEmptyBody(exchange)) return;
        String challengeId = AdminApiHttp.boundedHeader(exchange, "X-REvolution-Challenge", 64);
        String proof = AdminApiHttp.boundedHeader(exchange, "X-REvolution-Proof", 128);
        if (challengeId == null || proof == null) {
            AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        String token = auth.openSession(challengeId, proof);
        if (token == null) {
            AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        AdminApiHttp.respond(exchange, 200, "{\"token\":\"" + token + "\",\"idleTtlMillis\":300000,\"absoluteTtlMillis\":900000}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "GET") || !AdminApiHttp.requireEmptyBody(exchange)) return;
        String authorization = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
        if (!auth.authorizeBearer(authorization, AdminApiCapability.SERVER_READ)) {
            AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        String environment = System.getProperty("revolution.admin.environment", "UNCONFIGURED");
        if (!"DEV".equals(environment) && !"PROD".equals(environment)) environment = "UNCONFIGURED";
        AdminApiHttp.respond(exchange, 200, "{\"apiVersion\":1,\"modVersion\":\"" + RevolutionMod.VERSION
                + "\",\"remoteMode\":false,\"environment\":\"" + environment
                + "\",\"installationId\":\"" + installationId + "\",\"capabilities\":"
                + (tools == null ? "[\"mechanics.read\"]" : "[\"stages.read\",\"stages.write\",\"progression.read\",\"progression.write\",\"enhancements.write\",\"balance.read\",\"balance.write\",\"mechanics.read\"]") + "}");
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
