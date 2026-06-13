package com.cloudys.common.dto.project;

import java.time.Instant;

/**
 * 产品响应，对应 Python product_schemas。
 */
public record ProductResponse(
        String productId,
        String name,
        String description,
        String status,
        String version,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
