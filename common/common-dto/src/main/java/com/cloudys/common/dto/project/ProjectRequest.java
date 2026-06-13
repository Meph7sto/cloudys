package com.cloudys.common.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 项目创建/更新请求，对应 Python manage_schemas。
 */
public record ProjectRequest(
        @NotBlank(message = "项目名称不能为空")
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description,

        String status,

        String productId
) {}
