package com.cloudys.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectRegistrationRequest(
        @NotBlank(message = "拒绝原因不能为空") String reason
) {}
