package com.parcelflow.tracking.client;

import com.parcelflow.tracking.config.Config;
import com.parcelflow.tracking.exception.DownstreamException;
import com.parcelflow.tracking.exception.NotFoundException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies shipment exists via Shipment Service HTTP, not via DB table read.
 */
public class ShipmentClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public ShipmentClient() {
        this(Config.getShipmentServiceUrl());
    }

    public ShipmentClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    /**
     * Calls GET /api/v1/shipments/{trackingId} to verify existence.
     * @throws NotFoundException if 404
     * @throws DownstreamException if timeout/unavailable (503) or other error
     */
    public void verifyExists(String trackingId) {
        String url = baseUrl + "/api/v1/shipments/" + trackingId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code == 200) {
                return; // exists
            } else if (code == 404) {
                throw new NotFoundException("SHIPMENT_NOT_FOUND", "No shipment was found for the supplied tracking ID.");
            } else if (code == 400) {
                // Shipment service says malformed ID - propagate as validation
                throw new DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service rejected tracking ID", 503);
            } else {
                throw new DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service returned HTTP " + code, 503);
            }
        } catch (NotFoundException | DownstreamException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service timeout", 503, e);
        } catch (java.net.ConnectException e) {
            throw new DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service unavailable", 503, e);
        } catch (Exception e) {
            throw new DownstreamException("DOWNSTREAM_UNAVAILABLE", "Shipment service error: " + e.getMessage(), 503, e);
        }
    }
}
