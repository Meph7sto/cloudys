package com.cloudys.common.core.exception;

/**
 * 业务错误异常。
 * 抛出后被 GlobalExceptionHandler 统一处理为 {"detail": message} 格式，
 * 与 Python 版 ErrorResponse 语义保持一致。
 */
public class ErrorResponse extends RuntimeException {

    private final int statusCode;

    public ErrorResponse(String message) {
        this(message, 400);
    }

    public ErrorResponse(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDetail() {
        return getMessage();
    }
}
