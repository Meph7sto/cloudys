package com.cloudys.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBaselineRequest(
        @NotBlank(message = "版本号不能为空") String version,
        Boolean locked
) {}
