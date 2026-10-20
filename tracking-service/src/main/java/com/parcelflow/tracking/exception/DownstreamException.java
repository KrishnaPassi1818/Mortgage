package com.parcelflow.tracking.exception;
public class DownstreamException extends RuntimeException {
    private final String code;
    private final int statusCode;
    public DownstreamException(String code, String message, int statusCode) { super(message); this.code = code; this.statusCode = statusCode; }
    public DownstreamException(String code, String message, int statusCode, Throwable cause) { super(message, cause); this.code = code; this.statusCode = statusCode; }
    public String getCode() { return code; }
    public int getStatusCode() { return statusCode; }
}
