package com.cloudys.project.exception;

public class DownstreamServiceException extends RuntimeException {

    private final int statusCode;
    private final String detail;

    public DownstreamServiceException(int statusCode, String detail, Throwable cause) {
        super(detail, cause);
        this.statusCode = statusCode;
        this.detail = detail;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDetail() {
        return detail;
    }
}
