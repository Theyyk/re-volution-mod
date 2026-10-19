package com.example.revolution.admin.api;

import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.service.AdminOperationsService;
import com.example.revolution.admin.service.AdminQueryService;
import com.example.revolution.number.LargeNumbers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** Registers explicit versioned routes and delegates only to typed admin services on the server thread. */
final class AdminApiRoutes {
    private static final Set<String> RESOURCE_FIELDS = fields("uuid", "resource", "value");
    private static final Set<String> RESOURCE_ADD_FIELDS = fields("uuid", "resource", "amount");
    private static final Set<String> UUID_FIELDS = fields("uuid");
    private static final Set<String> RUNES_FILL_FIELDS = fields("uuid", "rank");
    private static final Set<String> MOB_CREATE_FIELDS = fields("name", "type", "resource", "amount", "hp", "x", "y", "z");
    private static final Set<String> MOB_NAME_FIELDS = fields("name");
    private static final Set<String> MOB_SPAWN_FIELDS = fields("name", "x", "y", "z");

    private final Logger logger;
    private final AdminApiAuthService auth;
    private final AdminOperationsService operations;
    private final AdminQueryService queries;
    private final MinecraftServer server;
    private final AdminServerThreadExecutor serverThread;

    AdminApiRoutes(Logger logger, AdminApiAuthService auth, AdminOperationsService operations,
                   AdminQueryService queries, MinecraftServer server) {
        this.logger = logger;
        this.auth = auth;
        this.operations = operations;
        this.queries = queries;
        this.server = server;
        this.serverThread = new AdminServerThreadExecutor(server);
    }

    void register(HttpServer http, RouteWrapper wrapper) {
        read(http, wrapper, "/api/v1/resources", AdminApiCapability.RESOURCES_READ,
                exchange -> AdminApiJson.resources(serverThread.call(queries::resourceNames)));
        read(http, wrapper, "/api/v1/players", AdminApiCapability.PLAYERS_READ,
                exchange -> {
                    if (!authorized(exchange, AdminApiCapability.RESOURCES_READ)
                            || !authorized(exchange, AdminApiCapability.RUNES_READ)) return null;
                    return AdminApiJson.players(serverThread.call(() -> queries.onlinePlayers(server.getPlayerList().getPlayers())));
                });
        read(http, wrapper, "/api/v1/mobs", AdminApiCapability.MOBS_READ,
                exchange -> AdminApiJson.mobs(serverThread.call(queries::mobs)));

        form(http, wrapper, "/api/v1/resources/set", AdminApiCapability.RESOURCES_WRITE, RESOURCE_FIELDS,
                (form, exchange) -> withOnlinePlayer(form.get("uuid"), player ->
                        operations.setResource(player, form.get("resource"), exactNumber(form.get("value"), false))));
        form(http, wrapper, "/api/v1/resources/add", AdminApiCapability.RESOURCES_WRITE, RESOURCE_ADD_FIELDS,
                (form, exchange) -> withOnlinePlayer(form.get("uuid"), player ->
                        operations.addResource(player, form.get("resource"), exactNumber(form.get("amount"), true))));
        form(http, wrapper, "/api/v1/resources/zero-all", AdminApiCapability.RESOURCES_WRITE, UUID_FIELDS,
                (form, exchange) -> withOnlinePlayer(form.get("uuid"), player -> {
                    operations.zeroAllResources(player);
                    return true;
                }));

        form(http, wrapper, "/api/v1/runes/clear", AdminApiCapability.RUNES_WRITE, UUID_FIELDS,
                (form, exchange) -> withOnlinePlayer(form.get("uuid"), player -> {
                    operations.clearRunes(player);
                    return true;
                }));
        form(http, wrapper, "/api/v1/runes/fill", AdminApiCapability.RUNES_WRITE, RUNES_FILL_FIELDS,
                (form, exchange) -> {
                    int rank = strictInt(form.get("rank"), 1, 3);
                    return withOnlinePlayer(form.get("uuid"), player -> {
                        operations.fillRunes(player, rank);
                        return true;
                    });
                });
        form(http, wrapper, "/api/v1/runes/reset-enhancements", AdminApiCapability.RUNES_WRITE, UUID_FIELDS,
                (form, exchange) -> withOnlinePlayer(form.get("uuid"), player -> {
                    operations.resetEnhancements(player);
                    return true;
                }));

        form(http, wrapper, "/api/v1/mobs/create", AdminApiCapability.MOBS_WRITE, MOB_CREATE_FIELDS,
                (form, exchange) -> operations.createMob(
                        form.get("name"), form.get("type"), form.get("resource"),
                        exactNumber(form.get("amount"), false), exactNumber(form.get("hp"), false),
                        coordinate(form.get("x"), false), coordinate(form.get("y"), true), coordinate(form.get("z"), false)));
        form(http, wrapper, "/api/v1/mobs/delete", AdminApiCapability.MOBS_WRITE, MOB_NAME_FIELDS,
                (form, exchange) -> operations.deleteMob(form.get("name")));
        form(http, wrapper, "/api/v1/mobs/set-spawn", AdminApiCapability.MOBS_WRITE, MOB_SPAWN_FIELDS,
                (form, exchange) -> operations.setMobSpawn(
                        form.get("name"), coordinate(form.get("x"), false), coordinate(form.get("y"), true), coordinate(form.get("z"), false)));
    }

    private void read(HttpServer http, RouteWrapper wrapper, String path, AdminApiCapability capability, ReadAction action) {
        http.createContext(path, wrapper.wrap(path, exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "GET") || !AdminApiHttp.requireEmptyBody(exchange)) return;
            if (!authorized(exchange, capability)) {
                AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            try {
                String result = action.execute(exchange);
                if (result != null) AdminApiHttp.respond(exchange, 200, result);
                else if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                    AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
                }
            } catch (Exception e) {
                throw asRuntime(e);
            }
        }));
    }

    private void form(HttpServer http, RouteWrapper wrapper, String path, AdminApiCapability capability,
                      Set<String> expectedFields, FormAction action) {
        http.createContext(path, wrapper.wrap(path, exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "POST")) return;
            if (!authorized(exchange, capability)) {
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
                form = AdminFormData.parse(body, expectedFields);
            } catch (IllegalArgumentException e) {
                AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
                return;
            }
            try {
                boolean changed = serverThread.call(() -> action.execute(form, exchange));
                if (!changed) {
                    AdminApiHttp.respond(exchange, 400, "{\"error\":\"rejected\"}");
                    audit(path, form, false);
                    return;
                }
                audit(path, form, true);
                AdminApiHttp.respond(exchange, 200, AdminApiJson.ok());
            } catch (PlayerOfflineException e) {
                AdminApiHttp.respond(exchange, 404, "{\"error\":\"player_offline\"}");
            } catch (IllegalArgumentException e) {
                AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
            } catch (Exception e) {
                throw asRuntime(e);
            }
        }));
    }

    private boolean authorized(HttpExchange exchange, AdminApiCapability capability) {
        return auth.authorizeBearer(AdminApiHttp.boundedHeader(exchange, "Authorization", 128), capability);
    }

    private boolean withOnlinePlayer(String rawUuid, PlayerAction action) throws Exception {
        UUID uuid = parseUuid(rawUuid);
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
        if (player == null) throw new PlayerOfflineException();
        return action.execute(player);
    }

    private void audit(String path, AdminFormData form, boolean success) {
        String target = form.get("uuid");
        if (target == null) target = sanitizeAudit(form.get("name"));
        logger.info("Admin API mutation operation=" + path + " target=" + (target == null ? "-" : target)
                + " result=" + (success ? "success" : "rejected"));
    }

    static BigDecimal exactNumber(String value, boolean signed) {
        if (value == null || value.isEmpty() || value.length() > 128) throw new IllegalArgumentException("invalid number");
        BigDecimal parsed;
        try {
            parsed = LargeNumbers.parseExact(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid number");
        }
        if (parsed.precision() > 100 || Math.abs((long) parsed.scale()) > 10_000L) throw new IllegalArgumentException("number out of bounds");
        if (!signed && parsed.signum() < 0) throw new IllegalArgumentException("negative number");
        return parsed;
    }

    static double coordinate(String value, boolean vertical) {
        if (value == null || value.length() > 64) throw new IllegalArgumentException("invalid coordinate");
        final double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid coordinate");
        }
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("invalid coordinate");
        double bound = vertical ? 4096.0D : 30_000_000.0D;
        if (Math.abs(parsed) > bound) throw new IllegalArgumentException("coordinate out of bounds");
        return parsed;
    }

    private static int strictInt(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) throw new IllegalArgumentException("integer out of range");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid integer");
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.length() != 36) throw new IllegalArgumentException("invalid uuid");
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid uuid");
        }
    }

    private static String sanitizeAudit(String value) {
        if (value == null) return null;
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < value.length() && safe.length() < 64; i++) {
            char c = value.charAt(i);
            if (c >= 0x20 && c != 0x7f) safe.append(c);
        }
        return safe.toString();
    }

    private static RuntimeException asRuntime(Exception e) {
        return e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin API operation failed", e);
    }

    private static Set<String> fields(String... names) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    interface RouteWrapper {
        HttpHandler wrap(String path, HttpHandler delegate);
    }

    private interface ReadAction { String execute(HttpExchange exchange) throws Exception; }
    private interface FormAction { boolean execute(AdminFormData form, HttpExchange exchange) throws Exception; }
    private interface PlayerAction { boolean execute(EntityPlayerMP player) throws Exception; }
    private static final class PlayerOfflineException extends Exception {}
}
