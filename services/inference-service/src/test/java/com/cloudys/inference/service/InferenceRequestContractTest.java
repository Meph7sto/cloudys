package com.cloudys.inference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudys.inference.dto.acquisition.ExtractionRequest;
import com.cloudys.inference.dto.chat.ChatCompletionRequest;
import com.cloudys.inference.dto.chat.ChatCompletionWithToolsRequest;
import com.cloudys.inference.dto.chat.ChatMessage;
import com.cloudys.inference.dto.chat.ChatRequest;
import com.cloudys.inference.dto.chat.ContextCompletionRequest;
import com.cloudys.inference.dto.classification.ClassificationRequest;
import com.cloudys.inference.dto.conflict.ConflictCheckRequest;
import com.cloudys.inference.dto.kb.KBSearchRequest;
import com.cloudys.inference.dto.l4.GenerateL4Request;
import com.cloudys.inference.dto.traceability.BatchTraceRelationRequest;
import com.cloudys.inference.dto.traceability.TraceRelationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class InferenceRequestContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void chatRequest_serializesWithSnakeCaseContract() {
        ChatRequest request = new ChatRequest("hello");

        Map<String, Object> json = mapper.convertValue(request, Map.class);

        assertThat(json)
                .containsEntry("system_prompt", "")
                .containsEntry("max_new_tokens", 512)
                .containsEntry("use_thinking_mode", true)
                .containsEntry("use_async_client", false);
    }

    @Test
    void dtoContracts_matchPythonFieldNames() {
        Map<String, Object> completionJson = mapper.convertValue(
                new ChatCompletionRequest(List.of(new ChatMessage("user", "hi", null, null, null)), null, null, null, null),
                Map.class);
        Map<String, Object> toolsJson = mapper.convertValue(
                new ChatCompletionWithToolsRequest(
                        List.of(new ChatMessage("user", "hi", "reason", "tool-1", List.of(Map.of("name", "x")))),
                        List.of(Map.of("type", "function")),
                        null,
                        null,
                        null,
                        null,
                        "auto",
                        null),
                Map.class);
        Map<String, Object> contextJson = mapper.convertValue(
                new ContextCompletionRequest("prompt", null, null, null, null),
                Map.class);
        Map<String, Object> classificationJson = mapper.convertValue(
                new ClassificationRequest(List.of("req"), null, null),
                Map.class);
        Map<String, Object> conflictJson = mapper.convertValue(
                new ConflictCheckRequest("A", "B", null, null),
                Map.class);
        Map<String, Object> kbJson = mapper.convertValue(
                new KBSearchRequest("q", null, 0, null),
                Map.class);
        Map<String, Object> traceJson = mapper.convertValue(
                new TraceRelationRequest("high", "low", 0),
                Map.class);
        Map<String, Object> batchTraceJson = mapper.convertValue(
                new BatchTraceRelationRequest(List.of("h"), List.of("l"), 0),
                Map.class);
        Map<String, Object> extractionJson = mapper.convertValue(
                new ExtractionRequest("text", null, null, null, null, null),
                Map.class);
        Map<String, Object> l4Json = mapper.convertValue(
                new GenerateL4Request(
                        List.of(new GenerateL4Request.TopRequirement("TOP-1", "Top req")),
                        null,
                        null,
                        null),
                Map.class);

        assertThat(completionJson).containsKeys("thinking_enabled", "max_tokens");
        assertThat(toolsJson).containsKeys("thinking_enabled", "max_tokens", "tool_choice", "use_async_client");
        assertThat(contextJson).containsKeys("system_prompt", "thinking_enabled", "max_tokens");
        assertThat(classificationJson).containsKeys("batch_size", "max_length");
        assertThat(conflictJson).containsKeys("requirement_a", "requirement_b", "use_thinking_mode");
        assertThat(kbJson).containsKeys("kb_type", "top_k");
        assertThat(traceJson).containsKeys("high_level_requirement", "low_level_requirement", "max_new_tokens");
        assertThat(batchTraceJson).containsKeys("high_level_requirements", "low_level_requirements", "max_new_tokens");
        assertThat(extractionJson).containsKeys("chunk_size", "clean_model", "extract_model", "use_thinking_mode");
        assertThat(l4Json).containsKey("use_thinking_mode");
    }
}
