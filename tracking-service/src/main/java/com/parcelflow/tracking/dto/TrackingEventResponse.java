package com.parcelflow.tracking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingEventResponse {
    private String status;
    private String location;
    private String note;
    private LocalDateTime eventTime;

    public TrackingEventResponse() {}

    public TrackingEventResponse(String status, String location, String note, LocalDateTime eventTime) {
        this.status = status;
        this.location = location;
        this.note = note;
        this.eventTime = eventTime;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
