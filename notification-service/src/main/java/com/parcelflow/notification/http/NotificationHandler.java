package com.parcelflow.notification.http;

import com.parcelflow.notification.dto.CreateNotificationRequest;
import com.parcelflow.notification.dto.NotificationResponse;
import com.parcelflow.notification.exception.ValidationException;
import com.parcelflow.notification.service.NotificationService;
import com.parcelflow.notification.util.HttpResponses;
import com.parcelflow.notification.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP handler for Notification Service.
 * POST /api/v1/notifications - create in-app notification
 * GET /api/v1/notifications?trackingId=... - list by trackingId
 */
public class NotificationHandler implements HttpHandler {

    private static final int MAX_BODY_BYTES = 8 * 1024;
    private final NotificationService service;

    public NotificationHandler(NotificationService service) {
        this.service = service;
    }

    public NotificationHandler() {
        this(new NotificationService());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.handleOptions(exchange);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            if ("POST".equalsIgnoreCase(method) && "/api/v1/notifications".equals(path)) {
                handleCreate(exchange);
            } else if ("GET".equalsIgnoreCase(method) && "/api/v1/notifications".equals(path)) {
                handleList(exchange, query);
            } else {
                HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found.");
            }
        } catch (ValidationException ve) {
            HttpResponses.sendError(exchange, 400, ve.getCode(), ve.getMessage());
        } catch (Exception e) {
            System.err.println("[NotificationHandler] Internal error: " + e.getMessage());
            e.printStackTrace();
            HttpResponses.sendError(exchange, 500, "INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
        } finally {
            try { exchange.getRequestBody().close(); } catch (Exception ignored) {}
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBodyWithLimit(exchange.getRequestBody());
        if (body == null) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Request body too large (max 8KB).");
            return;
        }
        if (body.trim().isEmpty()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Request body is required.");
            return;
        }

        CreateNotificationRequest req;
        try {
            req = Json.mapper().readValue(body, CreateNotificationRequest.class);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.length() > 200) msg = msg.substring(0, 200);
            if (msg == null) msg = "Invalid JSON";
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Malformed JSON: " + msg);
            return;
        }

        NotificationResponse resp = service.createNotification(req);
        HttpResponses.sendJson(exchange, 201, resp);
    }

    private void handleList(HttpExchange exchange, String query) throws IOException {
        Map<String, String> params = parseQuery(query);
        String trackingId = params.get("trackingId");

        if (trackingId == null || trackingId.trim().isEmpty()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "trackingId query parameter is required.");
            return;
        }
        // URL-decode already done in parseQuery, but double-check raw handling
        // Validation delegated to service (format check)
        List<NotificationResponse> list = service.listNotifications(trackingId);
        // Return array (empty if none) -> 200
        HttpResponses.sendJson(exchange, 200, list);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, value);
            } else if (!pair.isEmpty()) {
                String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                map.put(key, "");
            }
        }
        return map;
    }

    private String readBodyWithLimit(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        int total = 0;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = is.read(buf)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                return null;
            }
            baos.write(buf, 0, n);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
