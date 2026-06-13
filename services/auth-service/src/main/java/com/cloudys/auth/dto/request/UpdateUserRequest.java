package com.cloudys.auth.dto.request;

public record UpdateUserRequest(
        String displayName,
        String role,
        Boolean isActive
) {}
