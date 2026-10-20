package com.parcelflow.tracking;

import com.parcelflow.tracking.config.Config;
import com.parcelflow.tracking.http.HealthHandler;
import com.parcelflow.tracking.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class TrackingApplication {
    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", new HealthHandler());
        // Phase 4 will add /api/v1/tracking handlers

        try {
            String version = Database.checkConnection();
            System.out.println("[Tracking] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Tracking] WARNING: DB not reachable: " + e.getMessage());
        }

        server.setExecutor(null);
        server.start();
        System.out.println("Tracking Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
    }
}
