package com.cloudys.inference.dto.traceability;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 可追溯性关系分析请求（对应 Python TraceRelationRequest）。
 */
public record TraceRelationRequest(
        @JsonProperty("high_level_requirement") @NotBlank String highLevelRequirement,
        @JsonProperty("low_level_requirement") @NotBlank String lowLevelRequirement,
        @JsonProperty("max_new_tokens") int maxNewTokens
) {
    public TraceRelationRequest {
        if (maxNewTokens <= 0) maxNewTokens = 512;
    }
}
