package com.example.revolution.admin.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Strict HTTP boundary helpers shared by the small typed route handlers. */
final class AdminApiHttp {
    static final int MAX_FORM_BODY_BYTES = 4096;

    private AdminApiHttp() {}

    static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equals(exchange.getRequestMethod())) return true;
        exchange.getResponseHeaders().set("Allow", method);
        respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
        return false;
    }

    static boolean requireEmptyBody(HttpExchange exchange) throws IOException {
        String transferEncoding = exchange.getRequestHeaders().getFirst("Transfer-Encoding");
        String contentEncoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
        if (transferEncoding != null || contentEncoding != null) {
            respond(exchange, 413, "{\"error\":\"body_not_allowed\"}");
            return false;
        }
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength == null) return true;
        try {
            if (Long.parseLong(contentLength) == 0L) return true;
        } catch (NumberFormatException ignored) {
            respond(exchange, 400, "{\"error\":\"bad_request\"}");
            return false;
        }
        respond(exchange, 413, "{\"error\":\"body_not_allowed\"}");
        return false;
    }

    static String readFormBody(HttpExchange exchange) throws IOException {
        return readFormBody(exchange, MAX_FORM_BODY_BYTES);
    }

    static String readFormBody(HttpExchange exchange, int maximumBytes) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        if (headers.getFirst("Transfer-Encoding") != null || headers.getFirst("Content-Encoding") != null) return null;
        String contentType = headers.getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/x-www-form-urlencoded")) return null;
        String rawLength = headers.getFirst("Content-Length");
        if (rawLength == null) return null;
        int length;
        try {
            long parsed = Long.parseLong(rawLength);
            if (parsed < 0 || parsed > maximumBytes) return null;
            length = (int) parsed;
        } catch (NumberFormatException e) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, length));
        InputStream in = exchange.getRequestBody();
        byte[] buffer = new byte[512];
        int total = 0;
        while (total < length) {
            int read = in.read(buffer, 0, Math.min(buffer.length, length - total));
            if (read < 0) break;
            total += read;
            if (total > maximumBytes) return null;
            out.write(buffer, 0, read);
        }
        if (total != length || in.read() != -1) return null;
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static String boundedHeader(HttpExchange exchange, String name, int maxLength) {
        String value = exchange.getRequestHeaders().getFirst(name);
        if (value == null || value.length() > maxLength) return null;
        return value;
    }

    static boolean isBrowserOriginRequest(Headers headers) {
        return headers.getFirst("Origin") != null
                || headers.getFirst("Access-Control-Request-Method") != null
                || headers.getFirst("Sec-Fetch-Site") != null;
    }

    static void respond(HttpExchange exchange, int status, String json) throws IOException {
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

    static void safeInternalError(HttpExchange exchange) {
        try {
            respond(exchange, 500, "{\"error\":\"internal_error\"}");
        } catch (IOException ignored) {
            // Connection is already unusable; never expose exception details.
        }
    }
}
