package com.parcelflow.shipment.model;

import java.time.LocalDateTime;

/** Domain model — mirrors shipments table. Does not expose currentStatus (owned by Tracking). */
public class Shipment {
    private Long id;
    private String trackingId;
    private String senderName;
    private String recipientName;
    private String origin;
    private String destination;
    private LocalDateTime createdAt;

    public Shipment() {}

    public Shipment(String trackingId, String senderName, String recipientName, String origin, String destination, LocalDateTime createdAt) {
        this.trackingId = trackingId;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.origin = origin;
        this.destination = destination;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
