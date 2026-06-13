package com.cloudys.common.dto.auth;

import java.time.Instant;

/**
 * JWT Token 响应，对应 Python auth_schemas 的 TokenResponse。
 */
public record TokenResponse(
        String token,
        Instant expiresAt
) {}
