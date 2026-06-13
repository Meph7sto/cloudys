package com.cloudys.inference.dto.acquisition;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 需求抽取请求（对应 Python ExtractionRequest）。
 */
public record ExtractionRequest(
        @NotBlank String text,
        @JsonProperty("chunk_size") Integer chunkSize,
        Integer overlap,
        @JsonProperty("clean_model") String cleanModel,
        @JsonProperty("extract_model") String extractModel,
        @JsonProperty("use_thinking_mode") Boolean useThinkingMode
) {
    public ExtractionRequest {
        if (chunkSize == null || chunkSize <= 0) chunkSize = 2000;
        if (overlap == null || overlap < 0) overlap = 200;
        if (cleanModel == null || cleanModel.isBlank()) cleanModel = "deepseek-chat";
        if (extractModel == null || extractModel.isBlank()) extractModel = "deepseek-chat";
        if (useThinkingMode == null) useThinkingMode = true;
    }
}
