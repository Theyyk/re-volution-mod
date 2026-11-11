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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Explicit local DEV routes for mechanic-specific player state. */
final class AdminMechanicsRoutes {
    static final String PATH = "/api/v1/players/mechanics";
    static final String EDITABLE_PATH = "/api/v1/players/mechanics/editable";
    static final String SET_PATH = "/api/v1/players/mechanics/set";
    static final String TIME_SIMULATION_PATH = "/api/v1/players/mechanics/simulate-time";
    static final int SET_MAX_VALUE_LENGTH = AdminApiHttp.MAX_FORM_BODY_BYTES;
    private static final long MAX_DELTA_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    private static final int MAX_BAD_REQUEST_MESSAGE_LENGTH = 160;
    private static final Set<String> UUID_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("uuid")));
    private static final Set<String> SET_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("uuid", "section", "json")));
    private static final Set<String> TIME_SIMULATION_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("uuid", "deltaMillis")));
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
        http.createContext(EDITABLE_PATH, wrapper.wrap(EDITABLE_PATH, this::handleEditable));
        http.createContext(SET_PATH, wrapper.wrap(SET_PATH, this::handleSet));
        http.createContext(TIME_SIMULATION_PATH, wrapper.wrap(TIME_SIMULATION_PATH, this::handleTimeSimulation));
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!authorizePost(exchange)) return;
        AdminFormData form = parse(exchange, UUID_FIELDS);
        if (form == null) return;
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

    private void handleEditable(HttpExchange exchange) throws IOException {
        if (!authorizePost(exchange)) return;
        AdminFormData form = parse(exchange, UUID_FIELDS);
        if (form == null) return;
        try {
            UUID uuid = UUID.fromString(form.get("uuid"));
            String payload = serverThread.call(() -> JSON.toJson(mechanics.editableSnapshot(uuid)));
            AdminApiHttp.respond(exchange, 200, payload);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin mechanics editor read failed", e);
        }
    }

    private void handleSet(HttpExchange exchange) throws IOException {
        if (!authorizePost(exchange)) return;
        AdminFormData form = parse(exchange, SET_FIELDS, SET_MAX_VALUE_LENGTH);
        if (form == null) return;
        try {
            UUID uuid = UUID.fromString(form.get("uuid"));
            String section = form.get("section");
            String json = form.get("json");
            Map<String, Object> state = serverThread.call(() -> mechanics.replaceSection(uuid, section, json));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("section", section);
            result.put("state", state);
            AdminApiHttp.respond(exchange, 200, JSON.toJson(result));
        } catch (IllegalArgumentException e) {
            logBadRequest(form, e);
            AdminApiHttp.respond(exchange, 400, badRequestPayload(e));
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin mechanics editor write failed", e);
        }
    }

    private void handleTimeSimulation(HttpExchange exchange) throws IOException {
        if (!authorizePost(exchange)) return;
        AdminFormData form = parse(exchange, TIME_SIMULATION_FIELDS);
        if (form == null) return;
        try {
            UUID uuid = UUID.fromString(form.get("uuid"));
            long deltaMillis = Long.parseLong(form.get("deltaMillis"));
            if (deltaMillis < 0L || deltaMillis > MAX_DELTA_MILLIS) throw new IllegalArgumentException("deltaMillis out of range");
            long nowMillis = System.currentTimeMillis();
            String payload = serverThread.call(() -> JSON.toJson(mechanics.simulateGodTime(uuid, nowMillis, deltaMillis)));
            AdminApiHttp.respond(exchange, 200, payload);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, "{\"error\":\"bad_request\"}");
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException("Admin time simulation failed", e);
        }
    }

    static String badRequestPayload(IllegalArgumentException failure) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", "bad_request");
        String message = safeBadRequestMessage(failure == null ? null : failure.getMessage());
        if (message != null) result.put("message", message);
        return JSON.toJson(result);
    }

    private static void logBadRequest(AdminFormData form, IllegalArgumentException failure) {
        String uuid = form == null ? "-" : safeBadRequestMessage(form.get("uuid"));
        String section = form == null ? "-" : safeBadRequestMessage(form.get("section"));
        String message = safeBadRequestMessage(failure == null ? null : failure.getMessage());
        System.out.println("[admin-mechanics] set rejected uuid=" + (uuid == null ? "-" : uuid)
                + " section=" + (section == null ? "-" : section)
                + " message=" + (message == null ? "-" : message));
    }

    static String safeBadRequestMessage(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return null;
        StringBuilder safe = new StringBuilder(Math.min(trimmed.length(), MAX_BAD_REQUEST_MESSAGE_LENGTH));
        for (int i = 0; i < trimmed.length() && safe.length() < MAX_BAD_REQUEST_MESSAGE_LENGTH; i++) {
            char c = trimmed.charAt(i);
            if (c >= 32 && c <= 126) safe.append(c);
            else safe.append(' ');
        }
        String normalized = safe.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean authorizePost(HttpExchange exchange) throws IOException {
        if (!AdminApiHttp.requireMethod(exchange, "POST")) return false;
        String authorization = AdminApiHttp.boundedHeader(exchange, "Authorization", 128);
        if (!auth.authorizeBearer(authorization, AdminApiCapability.MECHANICS_READ)) {
            AdminApiHttp.respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return false;
        }
        return true;
    }

    private static AdminFormData parse(HttpExchange exchange, Set<String> fields) throws IOException {
        return parse(exchange, fields, 256);
    }

    private static AdminFormData parse(HttpExchange exchange, Set<String> fields, int maximumValueLength) throws IOException {
        String body = AdminApiHttp.readFormBody(exchange);
        if (body == null) {
            AdminApiHttp.respond(exchange, 413, "{\"error\":\"invalid_body\"}");
            return null;
        }
        try {
            return AdminFormData.parse(body, fields, maximumValueLength);
        } catch (IllegalArgumentException e) {
            AdminApiHttp.respond(exchange, 400, badRequestPayload(e));
            return null;
        }
    }
}
