package com.example.revolution.admin.api;

import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.service.AdminQueryService;
import com.example.revolution.effect.EffectResolver;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Read-only projection of the shared gameplay EffectResolver for one player. */
final class AdminEffectsRoutes {
    private static final String PATH = "/api/v1/players/effects";
    private static final Set<String> UUID_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("uuid")));
    private static final Gson JSON = new Gson();

    private final AdminQueryService queries;
    private final AdminApiAuthService auth;
    private final AdminServerThreadExecutor serverThread;

    AdminEffectsRoutes(AdminQueryService queries, AdminApiAuthService auth, MinecraftServer server) {
        if (queries == null || auth == null || server == null) throw new IllegalArgumentException("Effects route dependencies are required");
        this.queries = queries;
        this.auth = auth;
        this.serverThread = new AdminServerThreadExecutor(server);
    }

    void register(HttpServer http, AdminApiRoutes.RouteWrapper wrapper) {
        http.createContext(PATH, wrapper.wrap(PATH, this::handle));
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "POST")) return;
        String authorization = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
        if (!auth.authorizeBearer(authorization, AdminApiCapability.EFFECTS_READ)) {
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
            String payload = serverThread.call(() -> JSON.toJson(snapshot(uuid)));
            AdminApiHttp.respond(exchange, 200, payload);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin effects read failed", e);
        }
    }

    private Map<String, Object> snapshot(UUID uuid) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 1);
        result.put("playerUuid", uuid.toString());
        List<Map<String, Object>> effects = new ArrayList<>();
        for (EffectResolver.Stat stat : EffectResolver.Stat.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stat", stat.name());
            row.put("multiplier", queries.resolvedEffectMultiplier(uuid, stat));
            List<Map<String, Object>> contributions = new ArrayList<>();
            for (EffectResolver.Contribution contribution : queries.inspectResolvedEffect(uuid, stat)) {
                if (contribution == null || contribution.stat != stat) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("origin", contribution.origin);
                item.put("additiveMultiplier", contribution.additiveMultiplier);
                contributions.add(item);
            }
            row.put("contributions", contributions);
            effects.add(row);
        }
        result.put("effects", effects);
        return result;
    }
}
