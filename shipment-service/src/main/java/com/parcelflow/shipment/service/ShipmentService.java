package com.parcelflow.shipment.service;

import com.parcelflow.shipment.client.TrackingClient;
import com.parcelflow.shipment.dto.CreateShipmentRequest;
import com.parcelflow.shipment.dto.ShipmentResponse;
import com.parcelflow.shipment.exception.NotFoundException;
import com.parcelflow.shipment.exception.ValidationException;
import com.parcelflow.shipment.model.Shipment;
import com.parcelflow.shipment.repository.ShipmentRepository;
import com.parcelflow.shipment.util.TrackingIdGenerator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Business rules: validation, tracking ID generation, persistence.
 * No SQL here — delegates to repository. No HTTP here — delegates to client.
 */
public class ShipmentService {

    private final ShipmentRepository repository;
    private final TrackingClient trackingClient;

    public ShipmentService(ShipmentRepository repository, TrackingClient trackingClient) {
        this.repository = repository;
        this.trackingClient = trackingClient;
    }

    // Convenience constructor for production wiring
    public ShipmentService() {
        this(new ShipmentRepository(), new TrackingClient());
    }

    /**
     * Validate request fields per PRD FR-1.
     */
    public void validate(CreateShipmentRequest req) {
        if (req == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        validateField(req.getSenderName(), "senderName", 100);
        validateField(req.getRecipientName(), "recipientName", 100);
        validateField(req.getOrigin(), "origin", 120);
        validateField(req.getDestination(), "destination", 120);
    }

    private void validateField(String value, String fieldName, int maxLen) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " is required and must not be blank.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLen) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " must not exceed " + maxLen + " characters.");
        }
    }

    /**
     * Create shipment: validate, generate unique tracking ID, persist, try to init BOOKED.
     * Returns ShipmentResponse with trackingInitialized flag.
     */
    public ShipmentResponse createShipment(CreateShipmentRequest req) {
        validate(req);

        String trackingId = null;
        Shipment shipment = null;
        int attempts = 0;
        // Try up to 3 times for uniqueness collision
        while (attempts < 3) {
            trackingId = TrackingIdGenerator.generate();
            shipment = new Shipment(
                    trackingId,
                    req.getSenderName().trim(),
                    req.getRecipientName().trim(),
                    req.getOrigin().trim(),
                    req.getDestination().trim(),
                    LocalDateTime.now()
            );
            try {
                repository.insert(shipment);
                break; // success
            } catch (SQLException e) {
                // Check for duplicate key (MySQL error 1062)
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("duplicate") || e.getErrorCode() == 1062) {
                    attempts++;
                    System.out.println("[Shipment] Duplicate trackingId " + trackingId + ", retry " + attempts);
                    if (attempts >= 3) {
                        throw new RuntimeException("Failed to generate unique tracking ID after retries", e);
                    }
                    // retry with new ID
                } else {
                    System.err.println("[Shipment] DB insert failed: " + e.getMessage());
                    throw new RuntimeException("Database error during shipment creation", e);
                }
            }
        }

        if (shipment == null || shipment.getId() == null) {
            throw new RuntimeException("Failed to create shipment after retries");
        }

        // Phase 0 Q1-A: attempt to initialize BOOKED in Tracking, but don't fail the shipment if Tracking is down
        boolean trackingInitialized = false;
        try {
            trackingInitialized = trackingClient.initializeBooked(shipment.getTrackingId(), shipment.getOrigin());
        } catch (Exception e) {
            System.err.println("[Shipment] Tracking init threw: " + e.getMessage());
            trackingInitialized = false;
        }

        return toResponse(shipment, trackingInitialized);
    }

    public ShipmentResponse getShipment(String trackingId) {
        if (trackingId == null || trackingId.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "trackingId must not be blank.");
        }
        String tid = trackingId.trim();
        // Basic format check: allow alphanumeric 6-32, reject obviously malformed (spaces, slashes)
        if (tid.length() > 32 || tid.length() < 6 || !tid.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid tracking ID format.");
        }

        try {
            Optional<Shipment> opt = repository.findByTrackingId(tid);
            if (opt.isEmpty()) {
                throw new NotFoundException("SHIPMENT_NOT_FOUND", "No shipment was found for the supplied tracking ID.");
            }
            Shipment s = opt.get();
            return toResponse(s, null); // GET doesn't report trackingInitialized — only create does
        } catch (SQLException e) {
            System.err.println("[Shipment] DB lookup failed for " + tid + ": " + e.getMessage());
            throw new RuntimeException("Database error during shipment lookup", e);
        }
    }

    private ShipmentResponse toResponse(Shipment s, Boolean trackingInitialized) {
        ShipmentResponse r = new ShipmentResponse();
        r.setTrackingId(s.getTrackingId());
        r.setSenderName(s.getSenderName());
        r.setRecipientName(s.getRecipientName());
        r.setOrigin(s.getOrigin());
        r.setDestination(s.getDestination());
        r.setCreatedAt(s.getCreatedAt());
        r.setTrackingInitialized(trackingInitialized);
        return r;
    }
}
