package com.cloudys.inference.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Context Builder 专用补全请求（对应 Python ContextCompletionRequest）。
 */
public record ContextCompletionRequest(
        @NotBlank String prompt,
        @JsonProperty("system_prompt") String systemPrompt,
        String model,
        @JsonProperty("thinking_enabled") Boolean thinkingEnabled,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public ContextCompletionRequest {
        if (systemPrompt == null) systemPrompt = "";
        if (thinkingEnabled == null) thinkingEnabled = true;
        if (maxTokens == null || maxTokens <= 0) maxTokens = 8000;
    }
}
