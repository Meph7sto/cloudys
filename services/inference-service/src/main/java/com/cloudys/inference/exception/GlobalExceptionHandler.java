package com.cloudys.inference.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.cloudys.common.core.exception.ErrorResponse;

/**
 * 全局异常处理器（WebFlux）。
 * 统一返回 {"detail": message} 格式，与 Python FastAPI 风格一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务错误。
     */
    @ExceptionHandler(ErrorResponse.class)
    public ResponseEntity<Map<String, String>> handleErrorResponse(ErrorResponse ex) {
        log.warn("Business error: {}", ex.getDetail());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("detail", ex.getDetail()));
    }

    /**
     * Python 后端 HTTP 错误代理。
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleWebClientError(WebClientResponseException ex) {
        log.warn("Python backend error: {} {}", ex.getStatusCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("detail", ex.getResponseBodyAsString()));
    }

    /**
     * 兜底异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500)
                .body(Map.of("detail", "Internal Server Error"));
    }
}
