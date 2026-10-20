package com.parcelflow.shipment.dto;

public class CreateShipmentRequest {
    private String senderName;
    private String recipientName;
    private String origin;
    private String destination;

    public CreateShipmentRequest() {}

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}
