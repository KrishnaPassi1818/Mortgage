package com.parcelflow.notification.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private String trackingId;
    private String title;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private String deliveryState = "RECORDED";

    public Notification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDeliveryState() { return deliveryState; }
    public void setDeliveryState(String deliveryState) { this.deliveryState = deliveryState; }
}
