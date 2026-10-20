package com.parcelflow.shipment;

import com.parcelflow.shipment.config.Config;
import com.parcelflow.shipment.http.HealthHandler;
import com.parcelflow.shipment.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class ShipmentApplication {

    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/health", new HealthHandler());
        // Phase 2 will add /api/v1/shipments handlers

        // Basic DB check on startup (log, don't crash for Phase 1 demo)
        try {
            String version = Database.checkConnection();
            System.out.println("[Shipment] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Shipment] WARNING: DB not reachable: " + e.getMessage());
            System.out.println("[Shipment] Service will start but DB operations will fail until DB is available.");
        }

        server.setExecutor(null);
        server.start();
        System.out.println("Shipment Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
    }
}
