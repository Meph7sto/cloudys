package com.cloudys.inference.dto.conflict;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 冲突检测请求（对应 Python ConflictCheckRequest）。
 */
public record ConflictCheckRequest(
        @JsonProperty("requirement_a") @NotBlank String requirementA,
        @JsonProperty("requirement_b") @NotBlank String requirementB,
        String model,
        @JsonProperty("use_thinking_mode") Boolean useThinkingMode
) {
    public ConflictCheckRequest {
        if (model == null || model.isBlank()) model = "deepseek-chat";
        if (useThinkingMode == null) useThinkingMode = true;
    }
}
