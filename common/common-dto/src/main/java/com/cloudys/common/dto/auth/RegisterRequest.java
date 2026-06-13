package com.cloudys.common.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求，对应 Python auth_schemas 的 RegisterRequest。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度 3-50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度 6-100")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 100)
        @JsonAlias("display_name")
        String displayName,

        @Size(max = 50)
        String role
) {}
