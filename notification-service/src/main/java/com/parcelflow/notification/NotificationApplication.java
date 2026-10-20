package com.parcelflow.notification;

import com.parcelflow.notification.config.Config;
import com.parcelflow.notification.http.HealthHandler;
import com.parcelflow.notification.http.NotificationHandler;
import com.parcelflow.notification.repository.Database;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NotificationApplication {
    public static void main(String[] args) throws Exception {
        int port = Config.getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/v1/notifications", new NotificationHandler());

        try {
            String version = Database.checkConnection();
            System.out.println("[Notification] DB connected: " + version);
        } catch (Exception e) {
            System.out.println("[Notification] WARNING: DB not reachable: " + e.getMessage());
        }

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Notification Service listening on port " + port);
        System.out.println("  Health: http://localhost:" + port + "/health");
        System.out.println("  Create: POST http://localhost:" + port + "/api/v1/notifications");
        System.out.println("  List:   GET http://localhost:" + port + "/api/v1/notifications?trackingId=...");
    }
}
