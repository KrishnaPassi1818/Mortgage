package com.parcelflow.shipment;

import com.parcelflow.shipment.config.Config;
import com.parcelflow.shipment.http.HealthHandler;
import com.parcelflow.shipment.http.ShipmentHandler;
import com.parcelflow.shipment.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ShipmentApplication {

    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/health", new HealthHandler());
        server.createContext("/api/v1/shipments", new ShipmentHandler());
        // Also handle trailing slash variant and path-with-id under same context:
        // HttpServer matches prefix, so /api/v1/shipments handles both POST and GET/{id} via same handler

        // Basic DB check on startup (log, don't crash)
        try {
            String version = Database.checkConnection();
            System.out.println("[Shipment] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Shipment] WARNING: DB not reachable: " + e.getMessage());
            System.out.println("[Shipment] Service will start but DB operations will fail until DB is available.");
        }

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Shipment Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
        System.out.println("  Create: POST http://localhost:" + port + "/api/v1/shipments");
        System.out.println("  Lookup: GET http://localhost:" + port + "/api/v1/shipments/{trackingId}");
    }
}
