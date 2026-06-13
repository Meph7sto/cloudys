package com.cloudys.inference.controller;

import java.util.List;
import java.util.Map;

import com.cloudys.inference.dto.chat.ChatCompletionRequest;
import com.cloudys.inference.dto.chat.ChatMessage;
import com.cloudys.inference.dto.chat.ChatRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;
import com.cloudys.inference.service.InferenceRuntimeCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    private WebTestClient webTestClient;
    private InferenceOrchestratorService orchestrator;
    private InferenceRuntimeCoordinator runtimeCoordinator;

    @BeforeEach
    void setUp() {
        orchestrator = mock(InferenceOrchestratorService.class);
        runtimeCoordinator = mock(InferenceRuntimeCoordinator.class);
        webTestClient = WebTestClient.bindToController(new ChatController(orchestrator)).build();
    }

    @Test
    void completions_validRequest_returnsOk() {
        var request = new ChatCompletionRequest(
                List.of(new ChatMessage("user", "Hello", null, null, null)),
                "deepseek-chat", true, 0.7, 2000);

        when(orchestrator.chatCompletions(any()))
                .thenReturn(Mono.just(Map.of("content", "Hello from Python")));

        webTestClient.post()
                .uri("/api/v2/inference/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isEqualTo("Hello from Python");
    }

    @Test
    void completions_compatibilityPath_returnsOk() {
        var request = new ChatCompletionRequest(
                List.of(new ChatMessage("user", "Hello", null, null, null)),
                "deepseek-chat", true, 0.7, 2000);

        when(orchestrator.chatCompletions(any()))
                .thenReturn(Mono.just(Map.of("content", "Compat path ok")));

        webTestClient.post()
                .uri("/inference/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isEqualTo("Compat path ok");
    }

    @Test
    void streamChat_validRequest_returnsNdjson() {
        var request = new ChatRequest("Hello");

        when(orchestrator.streamChat(any()))
                .thenReturn(Flux.just(
                        "{\"type\":\"token\",\"content\":\"Hi\"}"));

        webTestClient.post()
                .uri("/api/v2/inference/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/x-ndjson");
    }
}
