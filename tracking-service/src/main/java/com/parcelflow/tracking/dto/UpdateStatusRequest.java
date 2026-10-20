package com.parcelflow.tracking.dto;

public class UpdateStatusRequest {
    private String status;
    private String location;
    private String note;

    public UpdateStatusRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
