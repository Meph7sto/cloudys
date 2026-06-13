package com.cloudys.project.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateProjectRequest(
        String name,
        String description,
        String status,
        @JsonAlias("product_id") String productId,
        @JsonAlias("current_session_id") String currentSessionId
) {}
