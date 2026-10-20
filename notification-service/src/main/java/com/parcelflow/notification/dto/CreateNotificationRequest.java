package com.parcelflow.notification.dto;

/**
 * In-app notification creation request.
 * Note: deliveryState is not supplied by client — always RECORDED.
 */
public class CreateNotificationRequest {
    private String trackingId;
    private String status;
    private String title;
    private String message;

    public CreateNotificationRequest() {}

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
