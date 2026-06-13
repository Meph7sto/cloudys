package com.cloudys.common.dto.project;

import java.time.Instant;

/**
 * 项目响应，对应 Python manage_schemas 的 ProjectResponse。
 */
public record ProjectResponse(
        String projectId,
        String name,
        String description,
        String status,
        String productId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
