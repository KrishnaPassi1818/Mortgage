package com.parcelflow.tracking.http;

import com.parcelflow.tracking.dto.TrackingResponse;
import com.parcelflow.tracking.dto.UpdateStatusRequest;
import com.parcelflow.tracking.exception.ConflictException;
import com.parcelflow.tracking.exception.DownstreamException;
import com.parcelflow.tracking.exception.NotFoundException;
import com.parcelflow.tracking.exception.ValidationException;
import com.parcelflow.tracking.service.TrackingService;
import com.parcelflow.tracking.util.HttpResponses;
import com.parcelflow.tracking.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Thin HTTP handler for Tracking Service.
 * GET /api/v1/tracking/{trackingId}
 * POST /api/v1/tracking/{trackingId}/events
 */
public class TrackingHandler implements HttpHandler {

    private static final int MAX_BODY_BYTES = 8 * 1024;
    private final TrackingService service;

    public TrackingHandler(TrackingService service) {
        this.service = service;
    }

    public TrackingHandler() {
        this(new TrackingService());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.handleOptions(exchange);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // Route: GET /api/v1/tracking/{trackingId}
            //        POST /api/v1/tracking/{trackingId}/events
            if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/tracking/") && !path.contains("/events")) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/tracking/") && path.endsWith("/events")) {
                handlePostEvent(exchange, path);
            } else {
                HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found. Expected GET /api/v1/tracking/{trackingId} or POST /api/v1/tracking/{trackingId}/events");
            }
        } catch (ValidationException ve) {
            // Distinguish INVALID_STATUS vs generic
            String code = ve.getCode() != null ? ve.getCode() : "VALIDATION_ERROR";
            HttpResponses.sendError(exchange, 400, code, ve.getMessage());
        } catch (NotFoundException ne) {
            HttpResponses.sendError(exchange, 404, ne.getCode(), ne.getMessage());
        } catch (ConflictException ce) {
            HttpResponses.sendError(exchange, 409, ce.getCode(), ce.getMessage());
        } catch (DownstreamException de) {
            HttpResponses.sendError(exchange, de.getStatusCode(), de.getCode(), de.getMessage());
        } catch (Exception e) {
            System.err.println("[TrackingHandler] Internal error: " + e.getMessage());
            e.printStackTrace();
            HttpResponses.sendError(exchange, 500, "INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
        } finally {
            try { exchange.getRequestBody().close(); } catch (Exception ignored) {}
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String prefix = "/api/v1/tracking/";
        if (path.length() <= prefix.length()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "trackingId is required in path.");
            return;
        }
        String rawId = path.substring(prefix.length());
        // Disallow extra slashes
        if (rawId.contains("/")) {
            HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Invalid path. Expected GET /api/v1/tracking/{trackingId}");
            return;
        }
        String trackingId;
        try {
            trackingId = URLDecoder.decode(rawId, StandardCharsets.UTF_8);
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Invalid tracking ID encoding.");
            return;
        }

        TrackingResponse resp = service.getTracking(trackingId);
        HttpResponses.sendJson(exchange, 200, resp);
    }

    private void handlePostEvent(HttpExchange exchange, String path) throws IOException {
        // Extract trackingId from /api/v1/tracking/{trackingId}/events
        String prefix = "/api/v1/tracking/";
        String suffix = "/events";
        if (!path.endsWith(suffix) || path.length() <= prefix.length() + suffix.length()) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "trackingId is required in path.");
            return;
        }
        String inner = path.substring(prefix.length(), path.length() - suffix.length());
        if (inner.isEmpty() || inner.contains("/")) {
            HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Invalid path. Expected POST /api/v1/tracking/{trackingId}/events");
            return;
        }
        String trackingId;
        try {
            trackingId = URLDecoder.decode(inner, StandardCharsets.UTF_8);
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Invalid tracking ID encoding.");
            return;
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

        UpdateStatusRequest req;
        try {
            req = Json.mapper().readValue(body, UpdateStatusRequest.class);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.length() > 200) msg = msg.substring(0, 200);
            if (msg == null) msg = "Invalid JSON";
            HttpResponses.sendError(exchange, 400, "VALIDATION_ERROR", "Malformed JSON: " + msg);
            return;
        }

        TrackingResponse resp = service.updateStatus(trackingId, req);
        // 200 OK with notificationRecorded flag - even if notification failed, status is committed (partial-success)
        HttpResponses.sendJson(exchange, 200, resp);
    }

    private String readBodyWithLimit(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        int total = 0;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = is.read(buf)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) return null;
            baos.write(buf, 0, n);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
