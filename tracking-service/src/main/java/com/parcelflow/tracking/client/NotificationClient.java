package com.parcelflow.tracking.client;

import com.parcelflow.tracking.config.Config;
import com.parcelflow.tracking.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Calls Notification Service after committed status update.
 * Finite timeouts, safe error handling. Returns boolean notificationRecorded.
 */
public class NotificationClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public NotificationClient() {
        this(Config.getNotificationServiceUrl());
    }

    public NotificationClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * Attempt to record notification. Never throws to caller - returns false on failure.
     * Called only after DB transaction commits.
     */
    public boolean recordNotification(String trackingId, String status, String location) {
        String url = baseUrl + "/api/v1/notifications";
        // Build title/message as in Architecture example
        String title = "Parcel status updated";
        String message = buildMessage(status, location);

        Map<String, String> body = Map.of(
                "trackingId", trackingId,
                "status", status,
                "title", title,
                "message", message
        );
        try {
            String json = Json.mapper().writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code == 201 || code == 200) {
                System.out.println("[Tracking] Notification recorded for " + trackingId + " status " + status);
                return true;
            } else {
                System.out.println("[Tracking] Notification failed for " + trackingId + " HTTP " + code + " body:" + resp.body());
                return false;
            }
        } catch (Exception e) {
            System.out.println("[Tracking] Notification exception for " + trackingId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    private String buildMessage(String status, String location) {
        String display = status.replace("_", " ").toLowerCase();
        // Simple mapping: capitalize first
        String cap = status.substring(0, 1) + status.substring(1).toLowerCase().replace("_", " ");
        // Use provided mapping for nicer messages
        switch (status) {
            case "BOOKED": return "Your parcel is booked. Tracking will update soon.";
            case "IN_TRANSIT": return "Your parcel is now in transit" + (location != null && !location.isBlank() ? " from " + location : "") + ".";
            case "OUT_FOR_DELIVERY": return "Your parcel is out for delivery" + (location != null && !location.isBlank() ? " from " + location : "") + ".";
            case "DELIVERED": return "Your parcel has been delivered.";
            case "DELAYED": return "Your parcel is delayed" + (location != null && !location.isBlank() ? " at " + location : "") + ". Please check back.";
            default: return "Your parcel status is now " + display + ".";
        }
    }
}
