package com.cloudys.inference.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 带工具定义的 Chat Completions 请求（对应 Python ChatCompletionWithToolsRequest）。
 */
public record ChatCompletionWithToolsRequest(
        @NotEmpty List<ChatMessage> messages,
        @NotEmpty List<Map<String, Object>> tools,
        String model,
        @JsonProperty("thinking_enabled") Boolean thinkingEnabled,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("tool_choice") String toolChoice,
        @JsonProperty("use_async_client") Boolean useAsyncClient
) {
    public ChatCompletionWithToolsRequest {
        if (model == null || model.isBlank()) model = "deepseek-chat";
        if (thinkingEnabled == null) thinkingEnabled = true;
        if (temperature == null) temperature = 0.3;
        if (maxTokens == null || maxTokens <= 0) maxTokens = 4096;
        if (useAsyncClient == null) useAsyncClient = false;
    }
}
