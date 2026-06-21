package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateUserRequest(
        @JsonAlias("display_name")
        String displayName,
        String role,
        @JsonAlias("is_active")
        Boolean isActive
) {}
