package com.cloudys.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100) String password,
        @NotBlank(message = "显示名称不能为空") @Size(max = 100) String displayName,
        String role,
        String externalType
) {}
