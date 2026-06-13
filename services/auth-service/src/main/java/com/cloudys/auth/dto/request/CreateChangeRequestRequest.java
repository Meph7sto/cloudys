package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChangeRequestRequest(
        @JsonAlias("baseline_id")
        @NotNull(message = "基线 ID 不能为空") Long baselineId,
        @NotBlank(message = "变更原因不能为空") String reason,
        @JsonAlias("change_summary")
        @NotBlank(message = "变更摘要不能为空") String changeSummary
) {}
