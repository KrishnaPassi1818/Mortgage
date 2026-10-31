package com.parcelflow.shipment.dto;

public class AdminProcessRequest {
    private String action; // APPROVE, REJECT, UPDATE
    private String adminNotes;
    private String destination; // optional update
    private String packageDetails; // optional update

    public AdminProcessRequest() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getPackageDetails() { return packageDetails; }
    public void setPackageDetails(String packageDetails) { this.packageDetails = packageDetails; }
}
