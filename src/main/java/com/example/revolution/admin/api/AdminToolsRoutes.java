package com.example.revolution.admin.api;

import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.service.AdminToolsService;
import com.example.revolution.admin.service.AdminStageBatchService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** Explicit typed routes, preserving the existing local authentication/server-thread boundary. */
final class AdminToolsRoutes {
    private static final Gson JSON = new Gson();
    private final AdminToolsService tools;
    private final com.example.revolution.admin.service.AdminOperationsService operations;
    private final AdminApiAuthService auth;
    private final AdminServerThreadExecutor thread;
    private final MinecraftServer server;
    private final Logger logger;
    private final AdminStageBatchService batch;
    AdminToolsRoutes(AdminToolsService tools, AdminStageBatchService batch, AdminApiAuthService auth, MinecraftServer server, Logger logger, com.example.revolution.admin.service.AdminOperationsService operations) {
        this.tools = tools; this.auth = auth; this.server = server; this.logger = logger;
        this.thread = new AdminServerThreadExecutor(server);
        this.batch = batch; this.operations = operations;
    }

    void register(HttpServer http, AdminApiRoutes.RouteWrapper wrapper) {
        http.createContext("/api/v1/stages", wrapper.wrap("/api/v1/stages", exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "GET") || !AdminApiHttp.requireEmptyBody(exchange)) return;
            if (!authorized(exchange, AdminApiCapability.STAGES_READ)) return;
            try { AdminApiHttp.respond(exchange, 200, JSON.toJson(thread.call(batch::list))); }
            catch (Exception e) { throw new IllegalStateException("Stage query failed", e); }
        }));
        form(http, wrapper, "/api/v1/players/grant-enhancements", AdminApiCapability.RUNES_WRITE, keys("uuid", "count"), true,
                form -> { operations.grantEnhancements(player(form.get("uuid")), integer(form.get("count"))); return ack(true); });
        batchForm(http, wrapper, "/api/v1/stages/preview", false);
        batchForm(http, wrapper, "/api/v1/stages/apply", true);
        form(http, wrapper, "/api/v1/players/progression", AdminApiCapability.PROGRESSION_READ, keys("uuid"), false,
                form -> {
                    EntityPlayerMP player = player(form.get("uuid"));
                    return Collections.singletonMap("stages", tools.progression(player.getUniqueID(), player.posX, player.posZ));
                });
        form(http, wrapper, "/api/v1/players/grant-stage", AdminApiCapability.PROGRESSION_WRITE, keys("uuid", "location", "stage"), true,
                form -> ack(tools.grant(player(form.get("uuid")).getUniqueID(), integer(form.get("location")), integer(form.get("stage")))));
        form(http, wrapper, "/api/v1/players/reset-current", AdminApiCapability.PROGRESSION_WRITE, keys("uuid"), true,
                form -> { EntityPlayerMP player = player(form.get("uuid")); return ack(tools.resetCurrent(player.getUniqueID(), player.posX, player.posZ)); });
        form(http, wrapper, "/api/v1/players/reset-location", AdminApiCapability.PROGRESSION_WRITE, keys("uuid", "location"), true,
                form -> ack(tools.resetLocation(player(form.get("uuid")).getUniqueID(), integer(form.get("location")))));
        form(http, wrapper, "/api/v1/players/reset-all", AdminApiCapability.PROGRESSION_WRITE, keys("uuid"), true,
                form -> ack(tools.resetAll(player(form.get("uuid")).getUniqueID())));
    }

    private void batchForm(HttpServer http, AdminApiRoutes.RouteWrapper wrapper, String path, boolean apply) {
        http.createContext(path, wrapper.wrap(path, exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "POST") || !authorized(exchange, AdminApiCapability.STAGES_WRITE)) return;
            String body = AdminApiHttp.readFormBody(exchange, 65536);
            if (body == null) { AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}"); return; }
            try {
                AdminFormData form = AdminFormData.parse(body, apply ? keys("token") : keys("revision", "patches"), apply ? 128 : 48000);
                // Session ownership is retained only in memory and never placed in logs/journal/preview JSON.
                String owner = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
                Object result = thread.call(() -> {
                    if (apply) { batch.apply(owner, form.get("token")); return Collections.singletonMap("ok", true); }
                    return batch.preview(owner, form.get("revision"), StagePatchJson.parse(form.get("patches")));
                }, apply ? 30 : 5);
                if (apply) logger.info("Admin API mutation operation=" + path + " result=success");
                AdminApiHttp.respond(exchange, 200, JSON.toJson(result));
            } catch (IllegalArgumentException e) { AdminApiHttp.respond(exchange, 409, "{\"error\":\"invalid_or_stale_preview\"}"); }
            catch (Exception e) { throw new IllegalStateException("Stage batch failed; verify configuration before retry", e); }
        }));
    }

    private void form(HttpServer http, AdminApiRoutes.RouteWrapper wrapper, String path, AdminApiCapability capability,
            Set<String> fields, boolean mutation, Operation operation) {
        http.createContext(path, wrapper.wrap(path, exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "POST") || !authorized(exchange, capability)) return;
            String body = AdminApiHttp.readFormBody(exchange);
            if (body == null) { AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}"); return; }
            try {
                AdminFormData form = AdminFormData.parse(body, fields);
                Object result = thread.call(() -> operation.run(form));
                if (mutation) logger.info("Admin API mutation operation=" + path + " result=success");
                AdminApiHttp.respond(exchange, 200, JSON.toJson(result));
            } catch (IllegalArgumentException e) {
                AdminApiHttp.respond(exchange, 400, "{\"error\":\"rejected\"}");
            } catch (Exception e) { throw new IllegalStateException("Typed admin request failed", e); }
        }));
    }

    private boolean authorized(HttpExchange exchange, AdminApiCapability capability) throws IOException {
        if (auth.authorizeBearer(AdminApiHttp.boundedHeader(exchange, "Authorization", 128), capability)) return true;
        AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}"); return false;
    }
    private EntityPlayerMP player(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw new IllegalArgumentException("Invalid UUID");
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(UUID.fromString(value));
        if (player == null) throw new IllegalArgumentException("Player offline");
        return player;
    }
    private static int integer(String value) {
        if (value == null || !value.matches("[0-9]{1,8}")) throw new IllegalArgumentException("Invalid integer");
        return Integer.parseInt(value);
    }
    private static Map<String, Boolean> ack(boolean ok) {
        if (!ok) throw new IllegalArgumentException("Mutation rejected");
        return Collections.singletonMap("ok", true);
    }
    private static Set<String> keys(String... values) { return new HashSet<>(Arrays.asList(values)); }
    private interface Operation { Object run(AdminFormData form) throws Exception; }
}
