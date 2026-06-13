package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @JsonAlias("old_password")
        @NotBlank(message = "旧密码不能为空") String oldPassword,
        @JsonAlias("new_password")
        @NotBlank(message = "新密码不能为空") @Size(min = 6, message = "新密码长度至少 6 位") String newPassword
) {}
