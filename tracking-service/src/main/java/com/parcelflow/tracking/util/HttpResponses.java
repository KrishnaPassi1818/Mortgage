package com.parcelflow.tracking.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public final class HttpResponses {
    private HttpResponses() {}
    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = Json.mapper().writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        addCors(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
    public static void sendError(HttpExchange exchange, int statusCode, String code, String message) throws IOException {
        Map<String, Object> err = Map.of("error", Map.of("code", code, "message", message));
        sendJson(exchange, statusCode, err);
    }
    public static void addCors(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String allowedOrigin = (requestOrigin != null && !requestOrigin.isBlank()) ? requestOrigin : com.parcelflow.tracking.config.Config.getFrontendOrigin();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().set("Vary", "Origin");
    }
    public static void handleOptions(HttpExchange exchange) throws IOException {
        addCors(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}
