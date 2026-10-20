package com.parcelflow.tracking;

import com.parcelflow.tracking.config.Config;
import com.parcelflow.tracking.http.HealthHandler;
import com.parcelflow.tracking.http.TrackingHandler;
import com.parcelflow.tracking.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class TrackingApplication {
    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/v1/tracking", new TrackingHandler());

        try {
            String version = Database.checkConnection();
            System.out.println("[Tracking] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Tracking] WARNING: DB not reachable: " + e.getMessage());
        }

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Tracking Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
        System.out.println("  Get:    GET http://localhost:" + port + "/api/v1/tracking/{trackingId}");
        System.out.println("  Update: POST http://localhost:" + port + "/api/v1/tracking/{trackingId}/events");
    }
}
