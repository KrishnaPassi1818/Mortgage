package com.parcelflow.shipment.http;

import com.parcelflow.shipment.util.HttpResponses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.handleOptions(exchange);
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponses.sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Only GET allowed for /health");
            return;
        }
        Map<String, String> body = Map.of("service", "shipment-service", "status", "UP");
        HttpResponses.sendJson(exchange, 200, body);
    }
}
