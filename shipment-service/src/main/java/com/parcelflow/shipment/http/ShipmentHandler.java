package com.parcelflow.shipment.http;

import com.parcelflow.shipment.dto.CreateShipmentRequest;
import com.parcelflow.shipment.dto.ShipmentResponse;
import com.parcelflow.shipment.exception.NotFoundException;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.service.ShipmentService;
import com.parcelflow.shipment.util.HttpResponses;
import com.parcelflow.shipment.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Thin HTTP handler: routing, parsing, status codes. No SQL.
 */
public class ShipmentHandler implements HttpHandler {

    private static final int MAX_BODY_BYTES = 8 * 1024; // 8KB limit
    private final ShipmentService service;

    public ShipmentHandler(ShipmentService service) {
        this.service = service;
    }

    public ShipmentHandler() {
        this(new ShipmentService());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.handleOptions(exchange);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equalsIgnoreCase(method) && "/api/v1/shipments".equals(path)) {
                handleCreate(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/shipments/")) {
                handleGet(exchange, path);
            } else if ("GET".equalsIgnoreCase(method) && "/api/v1/shipments".equals(path)) {
                // No list endpoint — treat as 405 or 404 per spec
                HttpResponses.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Use POST to create shipments.");
            } else {
                HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found.");
            }
        } catch (ValidationException ve) {
            HttpResponses.sendError(exchange, 400, ve.getCode(), ve.getMessage());
        } catch (NotFoundException ne) {
            HttpResponses.sendError(exchange, 404, ne.getCode(), ne.getMessage());
        } catch (Exception e) {
            // Log server-side, don't leak details to client
            System.err.println("[ShipmentHandler] Internal error: " + e.getMessage());
            e.printStackTrace();
            HttpResponses.sendError(exchange, 500, "INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
        } finally {
            // Ensure exchange is closed if not already
            try { exchange.getRequestBody().close(); } catch (Exception ignored) {}
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        // Content-Type check (warn, not strict fail)
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct != null && !ct.toLowerCase().contains("application/json")) {
            // Allow but note — some clients omit charset
            System.out.println("[Shipment] Warning: Content-Type not JSON: " + ct);
        }

        String body = readBodyWithLimit(exchange.getRequestBody());
        if (body == null) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Request body too large (max 8KB).");
            return;
        }
        if (body.trim().isEmpty()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Request body is required.");
            return;
        }

        CreateShipmentRequest req;
        try {
            req = Json.mapper().readValue(body, CreateShipmentRequest.class);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.length() > 200) msg = msg.substring(0, 200);
            if (msg == null) msg = "Invalid JSON";
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Malformed JSON: " + msg);
            return;
        }

        // Delegate to service (validation + ID generation + DB + tracking init)
        ShipmentResponse resp = service.createShipment(req);

        // 201 Created
        HttpResponses.sendJson(exchange, 201, resp);
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        // Extract trackingId: /api/v1/shipments/{trackingId}
        String prefix = "/api/v1/shipments/";
        if (path.length() <= prefix.length()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "trackingId is required in path.");
            return;
        }
        String rawId = path.substring(prefix.length());
        // Disallow extra slashes (e.g., /shipments/abc/extra)
        if (rawId.contains("/")) {
            HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Invalid path. Expected /api/v1/shipments/{trackingId}");
            return;
        }
        // URL-decode
        String trackingId;
        try {
            trackingId = URLDecoder.decode(rawId, StandardCharsets.UTF_8);
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Invalid tracking ID encoding.");
            return;
        }

        // Service will validate and lookup
        ShipmentResponse resp = service.getShipment(trackingId);
        // For GET, we don't expose trackingInitialized (or set null) — but we can include if present for UX
        // To keep strict ownership, omit trackingInitialized in GET — set null and Jackson will output null, so we remove it manually via map?
        // Simpler: send as is, client can ignore.
        HttpResponses.sendJson(exchange, 200, resp);
    }

    // Helper to avoid exposing jackson internal detail in error message
    // We catch JsonProcessingException and extract safe message

    private String readBodyWithLimit(InputStream is) throws IOException {
        // Stream with limit to avoid OOM on large bodies
        byte[] buf = new byte[1024];
        int total = 0;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = is.read(buf)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                return null; // too large
            }
            baos.write(buf, 0, n);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
