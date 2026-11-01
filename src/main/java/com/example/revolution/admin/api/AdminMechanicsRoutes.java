package com.example.revolution.admin.api;

import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.service.AdminMechanicsService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Explicit read-only routes for mechanic-specific player state. */
final class AdminMechanicsRoutes {
    private static final String PATH = "/api/v1/players/mechanics";
    private static final Set<String> UUID_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("uuid")));
    private static final Gson JSON = new Gson();

    private final AdminMechanicsService mechanics;
    private final AdminApiAuthService auth;
    private final AdminServerThreadExecutor serverThread;

    AdminMechanicsRoutes(AdminMechanicsService mechanics, AdminApiAuthService auth, MinecraftServer server) {
        if (mechanics == null || auth == null || server == null) throw new IllegalArgumentException("Mechanics route dependencies are required");
        this.mechanics = mechanics;
        this.auth = auth;
        this.serverThread = new AdminServerThreadExecutor(server);
    }

    void register(HttpServer http, AdminApiRoutes.RouteWrapper wrapper) {
        http.createContext(PATH, wrapper.wrap(PATH, this::handle));
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "POST")) return;
        String authorization = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
        if (!auth.authorizeBearer(authorization, AdminApiCapability.MECHANICS_READ)) {
            AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }

        String body = AdminApiHttp.readFormBody(exchange);
        if (body == null) {
            AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}");
            return;
        }

        final AdminFormData form;
        try {
            form = AdminFormData.parse(body, UUID_FIELDS);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
            return;
        }

        try {
            UUID uuid = UUID.fromString(form.get("uuid"));
            String payload = serverThread.call(() -> JSON.toJson(mechanics.snapshot(uuid)));
            AdminApiHttp.respond(exchange, 200, payload);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin mechanics read failed", e);
        }
    }
}
