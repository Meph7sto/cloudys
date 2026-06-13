package com.cloudys.common.dto.common;

import java.util.List;

/**
 * 通用分页响应。
 */
public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int size
) {
    public int getTotalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
