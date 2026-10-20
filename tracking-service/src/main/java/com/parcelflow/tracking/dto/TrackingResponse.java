package com.parcelflow.tracking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {
    private String trackingId;
    private String currentStatus;
    private Boolean notificationRecorded;
    private List<TrackingEventResponse> events;

    public TrackingResponse() {}

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public Boolean getNotificationRecorded() { return notificationRecorded; }
    public void setNotificationRecorded(Boolean notificationRecorded) { this.notificationRecorded = notificationRecorded; }
    public List<TrackingEventResponse> getEvents() { return events; }
    public void setEvents(List<TrackingEventResponse> events) { this.events = events; }
}
