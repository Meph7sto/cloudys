package com.cloudys.project.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        String roadmap,
        String version,
        List<String> tags
) {}
