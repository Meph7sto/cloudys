package com.cloudys.project.dto;

import java.util.List;

public record UpdateProductRequest(
        String name,
        String description,
        String status,
        String roadmap,
        String version,
        List<String> tags
) {}
