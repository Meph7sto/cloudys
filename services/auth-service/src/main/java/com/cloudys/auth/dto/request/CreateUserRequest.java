package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50) String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 100) String password,
        @NotBlank(message = "显示名称不能为空") @Size(max = 100) @JsonAlias("display_name") String displayName,
        String role,
        @JsonAlias("external_type") String externalType
) {}
