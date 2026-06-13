package com.cloudys.inference.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 通用聊天请求（对应 Python ChatRequest schema）。
 */
public record ChatRequest(
        @NotBlank String prompt,
        @JsonProperty("system_prompt") String systemPrompt,
        @JsonProperty("max_new_tokens") Integer maxNewTokens,
        String model,
        @JsonProperty("use_thinking_mode") Boolean useThinkingMode,
        @JsonProperty("use_async_client") Boolean useAsyncClient
) {
    public ChatRequest {
        if (systemPrompt == null) systemPrompt = "";
        if (maxNewTokens == null || maxNewTokens <= 0) maxNewTokens = 512;
        if (model == null || model.isBlank()) model = "deepseek-chat";
        if (useThinkingMode == null) useThinkingMode = true;
        if (useAsyncClient == null) useAsyncClient = false;
    }

    public ChatRequest(String prompt) {
        this(prompt, "", 512, "deepseek-chat", true, false);
    }
}
