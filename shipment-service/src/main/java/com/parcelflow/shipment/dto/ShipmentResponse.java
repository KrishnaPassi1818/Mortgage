package com.parcelflow.shipment.dto;

import java.time.LocalDateTime;

/** Response for shipment create/lookup — strict ownership: no currentStatus field. */
public class ShipmentResponse {
    private String trackingId;
    private String senderName;
    private String recipientName;
    private String origin;
    private String destination;
    private LocalDateTime createdAt;
    private Boolean trackingInitialized;

    public ShipmentResponse() {}

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
    public Boolean getTrackingInitialized() { return trackingInitialized; }
    public void setTrackingInitialized(Boolean trackingInitialized) { this.trackingInitialized = trackingInitialized; }
}
