package com.cloudys.inference.dto.chat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 聊天消息（对应 Python ChatMessage）。
 */
public record ChatMessage(
        String role,
        String content,
        @JsonProperty("reasoning_content") String reasoningContent,
        @JsonProperty("tool_call_id") String toolCallId,
        @JsonProperty("tool_calls") List<Map<String, Object>> toolCalls
) {
    public ChatMessage {
        if (role == null || role.isBlank()) role = "user";
    }
}
