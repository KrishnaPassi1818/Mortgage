package com.parcelflow.notification;

import com.parcelflow.notification.config.Config;
import com.parcelflow.notification.http.HealthHandler;
import com.parcelflow.notification.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class NotificationApplication {
    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", new HealthHandler());
        // Phase 3 will add /api/v1/notifications handlers

        try {
            String version = Database.checkConnection();
            System.out.println("[Notification] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Notification] WARNING: DB not reachable: " + e.getMessage());
        }

        server.setExecutor(null);
        server.start();
        System.out.println("Notification Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
    }
}
