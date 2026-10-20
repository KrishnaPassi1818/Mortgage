package com.parcelflow.tracking.service;

import com.parcelflow.tracking.client.NotificationClient;
import com.parcelflow.tracking.client.ShipmentClient;
import com.parcelflow.tracking.dto.TrackingEventResponse;
import com.parcelflow.tracking.dto.TrackingResponse;
import com.parcelflow.tracking.dto.UpdateStatusRequest;
import com.parcelflow.tracking.exception.ConflictException;
import com.parcelflow.tracking.exception.NotFoundException;
import com.parcelflow.tracking.exception.ValidationException;
import com.parcelflow.tracking.model.TrackingEvent;
import com.parcelflow.tracking.repository.TrackingRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core business rules: status validation, transition matrix, atomic DB transaction, downstream calls.
 */
public class TrackingService {

    // Canonical statuses
    public static final Set<String> CANONICAL = Set.of("BOOKED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED");

    // Transition matrix: from -> allowed to
    public static final Map<String, Set<String>> ALLOWED = Map.of(
            "BOOKED", Set.of("IN_TRANSIT", "DELAYED"),
            "IN_TRANSIT", Set.of("OUT_FOR_DELIVERY", "DELAYED", "DELIVERED"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED", "DELAYED", "IN_TRANSIT"),
            "DELAYED", Set.of("IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"),
            "DELIVERED", Set.of() // terminal
    );

    private final TrackingRepository repository;
    private final ShipmentClient shipmentClient;
    private final NotificationClient notificationClient;

    public TrackingService(TrackingRepository repository, ShipmentClient shipmentClient, NotificationClient notificationClient) {
        this.repository = repository;
        this.shipmentClient = shipmentClient;
        this.notificationClient = notificationClient;
    }

    public TrackingService() {
        this(new TrackingRepository(), new ShipmentClient(), new NotificationClient());
    }

    // For test injection of fakes
    public static TrackingService withClients(TrackingRepository repo, ShipmentClient sc, NotificationClient nc) {
        return new TrackingService(repo, sc, nc);
    }

    public void validateTrackingId(String trackingId) {
        if (trackingId == null || trackingId.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "trackingId must not be blank.");
        }
        String tid = trackingId.trim();
        if (tid.length() > 32 || tid.length() < 6 || !tid.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid tracking ID format.");
        }
    }

    public void validateUpdateRequest(UpdateStatusRequest req) {
        if (req == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "status is required and must not be blank.");
        }
        String status = req.getStatus().trim().toUpperCase();
        if (!CANONICAL.contains(status)) {
            throw new ValidationException("INVALID_STATUS", "Invalid status. Must be one of: BOOKED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, DELAYED.");
        }
        // Normalize to uppercase canonical
        req.setStatus(status);

        // Optional fields length checks
        if (req.getLocation() != null && req.getLocation().length() > 120) {
            throw new ValidationException("VALIDATION_ERROR", "location must not exceed 120 characters.");
        }
        if (req.getNote() != null && req.getNote().length() > 255) {
            throw new ValidationException("VALIDATION_ERROR", "note must not exceed 255 characters.");
        }
        // Trim location/note for storage (allow empty → store as null)
        if (req.getLocation() != null) {
            String trimmed = req.getLocation().trim();
            req.setLocation(trimmed.isEmpty() ? null : trimmed);
        }
        if (req.getNote() != null) {
            String trimmed = req.getNote().trim();
            req.setNote(trimmed.isEmpty() ? null : trimmed);
        }
    }

    public boolean isTransitionAllowed(String from, String to) {
        if (from == null) {
            // Only BOOKED allowed as first status
            return "BOOKED".equals(to);
        }
        Set<String> allowed = ALLOWED.get(from);
        if (allowed == null) return false;
        return allowed.contains(to);
    }

    /**
     * GET /api/v1/tracking/{trackingId}
     * Steps: validate ID, verify shipment exists via HTTP, check currentStatus, fetch events.
     */
    public TrackingResponse getTracking(String trackingId) {
        validateTrackingId(trackingId);
        String tid = trackingId.trim();

        // Verify shipment exists (do not read shipments table)
        shipmentClient.verifyExists(tid);

        try {
            Optional<String> currentOpt = repository.getCurrentStatus(tid);
            if (currentOpt.isEmpty()) {
                throw new NotFoundException("TRACKING_NOT_FOUND", "No tracking events found for the supplied tracking ID. Shipment exists but no tracking has been initialized.");
            }
            String currentStatus = currentOpt.get();
            List<TrackingEvent> events = repository.findEvents(tid);
            return toResponse(tid, currentStatus, null, events);
        } catch (SQLException e) {
            System.err.println("[TrackingService] DB error getTracking: " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * POST /api/v1/tracking/{trackingId}/events
     * Steps: validate, verify shipment, validate transition, atomic DB transaction, then notification.
     * Returns response with notificationRecorded flag (partial-success handling).
     */
    public TrackingResponse updateStatus(String trackingId, UpdateStatusRequest req) {
        validateTrackingId(trackingId);
        validateUpdateRequest(req);
        String tid = trackingId.trim();
        String newStatus = req.getStatus(); // already uppercased

        // 1. Verify shipment exists via HTTP (not DB)
        shipmentClient.verifyExists(tid);

        // 2. Get current status (if none, treat as null)
        String currentStatus = null;
        try {
            Optional<String> opt = repository.getCurrentStatus(tid);
            if (opt.isPresent()) currentStatus = opt.get();
        } catch (SQLException e) {
            System.err.println("[TrackingService] DB getCurrentStatus failed: " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }

        // 3. Validate transition
        if (!isTransitionAllowed(currentStatus, newStatus)) {
            if (currentStatus == null && !"BOOKED".equals(newStatus)) {
                throw new ConflictException("INVALID_TRANSITION", "First status must be BOOKED. Got " + newStatus);
            } else if ("DELIVERED".equals(currentStatus)) {
                throw new ConflictException("INVALID_TRANSITION", "Shipment already delivered. No further transitions allowed.");
            } else {
                throw new ConflictException("INVALID_TRANSITION", "Invalid status transition from " + (currentStatus != null ? currentStatus : "NONE") + " to " + newStatus);
            }
        }

        // 4. Atomic transaction: update shipment_status + insert tracking_events
        try {
            repository.updateStatusAtomically(tid, newStatus, req.getLocation(), req.getNote());
        } catch (SQLException e) {
            System.err.println("[TrackingService] DB transaction failed: " + e.getMessage());
            throw new RuntimeException("Database error during status update", e);
        }

        // 5. After commit, call Notification Service (do not hold transaction)
        boolean notificationRecorded = false;
        try {
            notificationRecorded = notificationClient.recordNotification(tid, newStatus, req.getLocation());
        } catch (Exception e) {
            System.err.println("[TrackingService] Notification call threw: " + e.getMessage());
            notificationRecorded = false;
        }

        // 6. Fetch updated events for response
        try {
            List<TrackingEvent> events = repository.findEvents(tid);
            // currentStatus now is newStatus (just committed)
            return toResponse(tid, newStatus, notificationRecorded, events);
        } catch (SQLException e) {
            System.err.println("[TrackingService] DB fetch after commit failed: " + e.getMessage());
            // Still return with known current status, empty events as fallback
            TrackingResponse fallback = new TrackingResponse();
            fallback.setTrackingId(tid);
            fallback.setCurrentStatus(newStatus);
            fallback.setNotificationRecorded(notificationRecorded);
            fallback.setEvents(List.of());
            return fallback;
        }
    }

    private TrackingResponse toResponse(String trackingId, String currentStatus, Boolean notificationRecorded, List<TrackingEvent> events) {
        TrackingResponse resp = new TrackingResponse();
        resp.setTrackingId(trackingId);
        resp.setCurrentStatus(currentStatus);
        resp.setNotificationRecorded(notificationRecorded);
        List<TrackingEventResponse> dtoEvents = events.stream()
                .map(e -> new TrackingEventResponse(e.getStatus(), e.getLocation(), e.getNote(), e.getEventTime()))
                .collect(Collectors.toList());
        resp.setEvents(dtoEvents);
        return resp;
    }
}
