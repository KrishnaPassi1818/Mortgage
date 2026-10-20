package com.parcelflow.tracking.model;

import java.time.LocalDateTime;

public class TrackingEvent {
    private Long id;
    private String trackingId;
    private String status;
    private String location;
    private String note;
    private LocalDateTime eventTime;

    public TrackingEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
