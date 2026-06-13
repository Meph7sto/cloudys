package com.cloudys.common.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 产品创建/更新请求，对应 Python product_schemas。
 */
public record ProductRequest(
        @NotBlank(message = "产品名称不能为空")
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        String status
) {}
