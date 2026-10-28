package com.example.revolution.admin.api;

import com.example.revolution.admin.api.auth.AdminApiAuthService;
import com.example.revolution.admin.api.auth.AdminApiCapability;
import com.example.revolution.admin.service.AdminBalanceBatchService;
import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.service.BalanceSnapshot;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

/** Authenticated Balance Core read/preview/apply routes. */
final class AdminBalanceRoutes {
    private static final Gson JSON = new Gson();
    private static final int MAX_BALANCE_BODY = 1_048_576;
    private static final int MAX_BALANCE_VALUE = 900_000;
    private final BalanceService balance;
    private final AdminBalanceBatchService batch;
    private final AdminApiAuthService auth;
    private final AdminServerThreadExecutor thread;
    private final Logger logger;
    private final BalanceConfigCodec codec = new BalanceConfigCodec();

    AdminBalanceRoutes(BalanceService balance, AdminBalanceBatchService batch, AdminApiAuthService auth,
            MinecraftServer server, Logger logger) {
        if (balance == null || batch == null || auth == null || server == null || logger == null) {
            throw new IllegalArgumentException("Admin balance route dependencies are required");
        }
        this.balance = balance;
        this.batch = batch;
        this.auth = auth;
        this.thread = new AdminServerThreadExecutor(server);
        this.logger = logger;
    }

    void register(HttpServer http, AdminApiRoutes.RouteWrapper wrapper) {
        http.createContext("/api/v1/balance", wrapper.wrap("/api/v1/balance", exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "GET") || !AdminApiHttp.requireEmptyBody(exchange)) return;
            if (!authorized(exchange, AdminApiCapability.BALANCE_READ)) return;
            try {
                AdminApiHttp.respond(exchange, 200, JSON.toJson(thread.call(this::current)));
            } catch (Exception e) {
                throw new IllegalStateException("Balance query failed", e);
            }
        }));
        http.createContext("/api/v1/balance/preview", wrapper.wrap("/api/v1/balance/preview", exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "POST") || !authorized(exchange, AdminApiCapability.BALANCE_WRITE)) return;
            String body = AdminApiHttp.readFormBody(exchange, MAX_BALANCE_BODY);
            if (body == null) { AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}"); return; }
            try {
                AdminFormData form = AdminFormData.parse(body, keys("revision", "config"), MAX_BALANCE_VALUE);
                String owner = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
                Object result = thread.call(() -> batch.preview(owner, form.get("revision"), form.get("config")), 10);
                AdminApiHttp.respond(exchange, 200, JSON.toJson(result));
            } catch (IllegalArgumentException e) {
                AdminApiHttp.respond(exchange, 409, "{\"error\":\"invalid_or_stale_preview\"}");
            } catch (Exception e) {
                throw new IllegalStateException("Balance preview failed", e);
            }
        }));
        http.createContext("/api/v1/balance/apply", wrapper.wrap("/api/v1/balance/apply", exchange -> {
            if (!AdminApiHttp.requireMethod(exchange, "POST") || !authorized(exchange, AdminApiCapability.BALANCE_WRITE)) return;
            String body = AdminApiHttp.readFormBody(exchange);
            if (body == null) { AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}"); return; }
            try {
                AdminFormData form = AdminFormData.parse(body, keys("token"), 128);
                String owner = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
                BalanceSnapshot activated = thread.call(() -> batch.apply(owner, form.get("token")), 10);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("ok", true);
                result.put("revision", activated.getRevision());
                result.put("balanceSchemaVersion", activated.getConfig().getBalanceSchemaVersion());
                logger.info("Admin API mutation operation=/api/v1/balance/apply result=success");
                AdminApiHttp.respond(exchange, 200, JSON.toJson(result));
            } catch (IllegalArgumentException e) {
                AdminApiHttp.respond(exchange, 409, "{\"error\":\"invalid_or_stale_preview\"}");
            } catch (Exception e) {
                throw new IllegalStateException("Balance apply failed", e);
            }
        }));
    }

    private Map<String, Object> current() {
        BalanceSnapshot active = balance.getActiveSnapshot();
        if (active == null) throw new IllegalStateException("no active balance config");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revision", active.getRevision());
        result.put("balanceSchemaVersion", active.getConfig().getBalanceSchemaVersion());
        result.put("config", codec.canonicalJson(active.getConfig()));
        return result;
    }

    private boolean authorized(HttpExchange exchange, AdminApiCapability capability) throws IOException {
        if (auth.authorizeBearer(AdminApiHttp.boundedHeader(exchange, "Authorization", 128), capability)) return true;
        AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
        return false;
    }

    private static Set<String> keys(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
