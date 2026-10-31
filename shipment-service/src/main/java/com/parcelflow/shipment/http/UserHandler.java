package com.parcelflow.shipment.http;

import com.parcelflow.shipment.dto.AdminProcessRequest;
import com.parcelflow.shipment.dto.CreateShipmentOrderRequest;
import com.parcelflow.shipment.dto.LoginRequest;
import com.parcelflow.shipment.dto.SignupRequest;
import com.parcelflow.shipment.exception.NotFoundException;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.service.UserService;
import com.parcelflow.shipment.util.HttpResponses;
import com.parcelflow.shipment.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UserHandler implements HttpHandler {

    private static final int MAX_BODY_BYTES = 16 * 1024;
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public UserHandler() {
        this(new UserService());
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
            // Routing:
            // POST /api/v1/auth/signup
            // POST /api/v1/auth/login
            // GET  /api/v1/users/{id}
            // POST /api/v1/requests
            // GET  /api/v1/requests (all, for admin) or GET /api/v1/requests?userId=123
            // PUT/PATCH /api/v1/requests/{id} (admin approve / update)
            if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/signup".equals(path)) {
                handleSignup(exchange);
            } else if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(path)) {
                handleLogin(exchange);
            } else if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/users/")) {
                handleGetUser(exchange, path);
            } else if ("POST".equalsIgnoreCase(method) && "/api/v1/requests".equals(path)) {
                handleCreateRequest(exchange);
            } else if ("GET".equalsIgnoreCase(method) && "/api/v1/requests".equals(path)) {
                handleGetRequests(exchange);
            } else if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))
                    && path.startsWith("/api/v1/requests/")) {
                handleProcessRequest(exchange, path);
            } else {
                HttpResponses.sendError(exchange, 404, "NOT_FOUND", "Endpoint not found: " + path);
            }
        } catch (ValidationException ve) {
            HttpResponses.sendError(exchange, 400, ve.getCode(), ve.getMessage());
        } catch (NotFoundException ne) {
            HttpResponses.sendError(exchange, 404, ne.getCode(), ne.getMessage());
        } catch (Exception e) {
            System.err.println("[UserHandler] Internal error: " + e.getMessage());
            e.printStackTrace();
            HttpResponses.sendError(exchange, 500, "INTERNAL_ERROR", "An unexpected error occurred. Please try again.");
        } finally {
            try { exchange.getRequestBody().close(); } catch (Exception ignored) {}
        }
    }

    private void handleSignup(HttpExchange exchange) throws IOException {
        String body = readBody(exchange.getRequestBody());
        SignupRequest req = Json.mapper().readValue(body, SignupRequest.class);
        var resp = userService.signup(req);
        HttpResponses.sendJson(exchange, 201, resp);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange.getRequestBody());
        LoginRequest req = Json.mapper().readValue(body, LoginRequest.class);
        var resp = userService.login(req);
        HttpResponses.sendJson(exchange, 200, resp);
    }

    private void handleGetUser(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring("/api/v1/users/".length());
        Long id = Long.parseLong(idStr);
        var resp = userService.getUserProfile(id);
        HttpResponses.sendJson(exchange, 200, resp);
    }

    private void handleCreateRequest(HttpExchange exchange) throws IOException {
        String body = readBody(exchange.getRequestBody());
        CreateShipmentOrderRequest req = Json.mapper().readValue(body, CreateShipmentOrderRequest.class);
        var resp = userService.createShipmentOrder(req);
        HttpResponses.sendJson(exchange, 201, resp);
    }

    private void handleGetRequests(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("userId=")) {
            String userIdStr = query.replace("userId=", "").split("&")[0];
            Long userId = Long.parseLong(userIdStr);
            var list = userService.getUserShipmentRequests(userId);
            HttpResponses.sendJson(exchange, 200, list);
        } else {
            var list = userService.getAllShipmentRequests();
            HttpResponses.sendJson(exchange, 200, list);
        }
    }

    private void handleProcessRequest(HttpExchange exchange, String path) throws IOException {
        String idStr = path.substring("/api/v1/requests/".length());
        if (idStr.contains("/")) {
            idStr = idStr.substring(0, idStr.indexOf("/"));
        }
        Long requestId = Long.parseLong(idStr);
        String body = readBody(exchange.getRequestBody());
        AdminProcessRequest req = Json.mapper().readValue(body, AdminProcessRequest.class);
        var resp = userService.processShipmentRequest(requestId, req);
        HttpResponses.sendJson(exchange, 200, resp);
    }

    private String readBody(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        int total = 0;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = is.read(buf)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) {
                throw new ValidationException("VALIDATION_ERROR", "Request body too large.");
            }
            baos.write(buf, 0, n);
        }
        String s = baos.toString(StandardCharsets.UTF_8);
        if (s.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        return s;
    }
}
