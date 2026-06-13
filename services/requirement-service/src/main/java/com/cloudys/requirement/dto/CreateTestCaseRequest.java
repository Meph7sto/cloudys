package com.cloudys.requirement.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTestCaseRequest(
        @NotBlank String title,
        String description,
        String status,
        String source
) {}
