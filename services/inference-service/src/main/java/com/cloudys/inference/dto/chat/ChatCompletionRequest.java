package com.cloudys.inference.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chat Completions 请求（对应 Python ChatCompletionRequest）。
 */
public record ChatCompletionRequest(
        @NotEmpty List<ChatMessage> messages,
        String model,
        @JsonProperty("thinking_enabled") Boolean thinkingEnabled,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public ChatCompletionRequest {
        if (model == null || model.isBlank()) model = "deepseek-chat";
        if (thinkingEnabled == null) thinkingEnabled = true;
        if (temperature == null || temperature <= 0) temperature = 0.7;
        if (maxTokens == null || maxTokens <= 0) maxTokens = 2000;
    }
}
