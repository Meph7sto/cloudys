package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record ImportRequirementsRequest(
        @NotBlank @JsonAlias("session_id") String sessionId,
        @JsonAlias("mapping_mode") String mappingMode
) {}
