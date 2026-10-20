package com.parcelflow.shipment.client;

import com.parcelflow.shipment.config.Config;
import com.parcelflow.shipment.util.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Calls Tracking Service to initialize BOOKED event.
 * Must NOT break shipment creation if Tracking is unavailable.
 * Returns true if initialization succeeded (2xx), false otherwise.
 */
public class TrackingClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public TrackingClient() {
        this(Config.getTrackingServiceUrl());
    }

    public TrackingClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * Attempts to create initial BOOKED event in Tracking Service.
     * @param trackingId generated tracking ID
     * @param origin shipment origin for location field
     * @return true if Tracking responded 2xx, false if failed/timeout/non-2xx
     */
    public boolean initializeBooked(String trackingId, String origin) {
        String url = baseUrl + "/api/v1/tracking/" + trackingId + "/events";
        Map<String, String> body = Map.of(
                "status", "BOOKED",
                "location", origin != null ? origin : "",
                "note", "Shipment registered"
        );
        try {
            String json = Json.mapper().writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                System.out.println("[Shipment] Tracking init succeeded for " + trackingId + " -> " + code);
                return true;
            } else {
                System.out.println("[Shipment] Tracking init failed for " + trackingId + " -> HTTP " + code + " body: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.out.println("[Shipment] Tracking init exception for " + trackingId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
}
