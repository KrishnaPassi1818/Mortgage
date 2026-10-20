package com.parcelflow.notification.http;

import com.parcelflow.notification.util.HttpResponses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { HttpResponses.handleOptions(exchange); return; }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { HttpResponses.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Only GET allowed for /health"); return; }
        HttpResponses.sendJson(exchange, 200, Map.of("service", "notification-service", "status", "UP"));
    }
}
