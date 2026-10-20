package com.parcelflow.notification.service;

import com.parcelflow.notification.dto.CreateNotificationRequest;
import com.parcelflow.notification.dto.NotificationResponse;
import com.parcelflow.notification.exception.ValidationException;
import com.parcelflow.notification.model.Notification;
import com.parcelflow.notification.repository.NotificationRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business rules for in-app notifications only (no email/SMS).
 * Validates required fields, enforces canonical status values, persists via JDBC.
 */
public class NotificationService {

    private static final Set<String> CANONICAL_STATUSES = Set.of(
            "BOOKED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED"
    );

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public NotificationService() {
        this(new NotificationRepository());
    }

    public void validateCreate(CreateNotificationRequest req) {
        if (req == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required.");
        }
        validateTrackingId(req.getTrackingId());
        validateField(req.getStatus(), "status", 32);
        validateStatus(req.getStatus());
        validateField(req.getTitle(), "title", 120);
        validateField(req.getMessage(), "message", 255);
    }

    public void validateTrackingId(String trackingId) {
        if (trackingId == null || trackingId.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "trackingId is required and must not be blank.");
        }
        String tid = trackingId.trim();
        if (tid.length() > 32 || tid.length() < 6 || !tid.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid tracking ID format.");
        }
    }

    private void validateField(String value, String fieldName, int maxLen) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " is required and must not be blank.");
        }
        if (value.trim().length() > maxLen) {
            throw new ValidationException("VALIDATION_ERROR", fieldName + " must not exceed " + maxLen + " characters.");
        }
    }

    private void validateStatus(String status) {
        String trimmed = status.trim().toUpperCase();
        if (!CANONICAL_STATUSES.contains(trimmed)) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid status. Must be one of: BOOKED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, DELAYED.");
        }
    }

    public NotificationResponse createNotification(CreateNotificationRequest req) {
        validateCreate(req);

        Notification n = new Notification();
        n.setTrackingId(req.getTrackingId().trim());
        n.setStatus(req.getStatus().trim().toUpperCase());
        n.setTitle(req.getTitle().trim());
        n.setMessage(req.getMessage().trim());
        n.setDeliveryState("RECORDED");

        try {
            repository.insert(n);
            // Verify inserted by fetching createdAt via DB timestamp (use now as close approximation, or query)
            // For accuracy, we fetch the inserted row if possible; simplest: set to now and trust DB is close
            // Better: do a quick lookup to get actual DB timestamp
            // Let's try to get timestamp from DB by querying findByTrackingId and filtering by id
            // But we already have list; instead just set to now for response
            if (n.getCreatedAt() == null) {
                // If not set by DB retrieval, set to now
                n.setCreatedAt(LocalDateTime.now());
            }
            // Attempt to fetch actual DB timestamp for this id (best effort)
            try {
                List<Notification> all = repository.findByTrackingId(n.getTrackingId());
                for (Notification found : all) {
                    if (found.getId().equals(n.getId())) {
                        n.setCreatedAt(found.getCreatedAt());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // keep current timestamp
            }
        } catch (SQLException e) {
            System.err.println("[Notification] DB insert failed: " + e.getMessage());
            throw new RuntimeException("Database error during notification creation", e);
        }

        return toResponse(n);
    }

    public List<NotificationResponse> listNotifications(String trackingId) {
        validateTrackingId(trackingId);
        try {
            List<Notification> list = repository.findByTrackingId(trackingId.trim());
            return list.stream().map(this::toResponse).collect(Collectors.toList());
        } catch (SQLException e) {
            System.err.println("[Notification] DB list failed: " + e.getMessage());
            throw new RuntimeException("Database error during notification lookup", e);
        }
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setTrackingId(n.getTrackingId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setStatus(n.getStatus());
        r.setCreatedAt(n.getCreatedAt());
        r.setDeliveryState(n.getDeliveryState());
        return r;
    }
}
