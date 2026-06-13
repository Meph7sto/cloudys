package com.cloudys.common.core.util;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * API 响应工具类，与 Python FastAPI 行为一致：
 * 成功直接返回数据体，错误返回 {"detail": message}。
 */
public final class ApiResponse {

    private ApiResponse() {}

    public static <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    public static <T> ResponseEntity<T> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }

    public static ResponseEntity<Map<String, String>> error(String message, int status) {
        return ResponseEntity.status(status).body(Map.of("detail", message));
    }

    public static ResponseEntity<Map<String, String>> badRequest(String message) {
        return error(message, 400);
    }

    public static ResponseEntity<Map<String, String>> unauthorized(String message) {
        return error(message, 401);
    }

    public static ResponseEntity<Map<String, String>> forbidden(String message) {
        return error(message, 403);
    }

    public static ResponseEntity<Map<String, String>> notFound(String message) {
        return error(message, 404);
    }

    public static ResponseEntity<Map<String, String>> internalError(String message) {
        return error(message, 500);
    }
}
