package com.cloudys.common.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 通用分页请求。
 */
public record PageRequest(
        @Min(1) int page,
        @Min(1) @Max(100) int size
) {
    public PageRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }

    public int getOffset() {
        return (page - 1) * size;
    }
}
