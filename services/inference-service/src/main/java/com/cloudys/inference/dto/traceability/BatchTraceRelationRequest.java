package com.cloudys.inference.dto.traceability;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 批量可追溯性关系分析请求（对应 Python BatchTraceRelationRequest）。
 */
public record BatchTraceRelationRequest(
        @JsonProperty("high_level_requirements") @NotEmpty List<String> highLevelRequirements,
        @JsonProperty("low_level_requirements") @NotEmpty List<String> lowLevelRequirements,
        @JsonProperty("max_new_tokens") int maxNewTokens
) {
    public BatchTraceRelationRequest {
        if (maxNewTokens <= 0) maxNewTokens = 512;
    }
}
