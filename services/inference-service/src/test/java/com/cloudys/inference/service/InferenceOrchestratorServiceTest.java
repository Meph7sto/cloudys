package com.cloudys.inference.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;

import com.cloudys.common.pythonbridge.PythonBridgeClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class InferenceOrchestratorServiceTest {

    @Test
    void streamCheckConflict_usesHyphenatedPythonRoute() {
        PythonBridgeClient bridge = mock(PythonBridgeClient.class);
        when(bridge.postStreamRaw(eq("/conflict/stream-check"), eq(Map.of("a", 1))))
                .thenReturn(Flux.just("{}"));

        InferenceOrchestratorService service = new InferenceOrchestratorService(bridge);

        service.streamCheckConflict(Map.of("a", 1)).collectList().block();

        verify(bridge).postStreamRaw("/conflict/stream-check", Map.of("a", 1));
    }

    @Test
    void validateL4_usesPythonValidateRoute() {
        PythonBridgeClient bridge = mock(PythonBridgeClient.class);
        when(bridge.post(eq("/l4/validate"), eq(Map.of("l4_requirements", java.util.List.of())), org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("global_pass", true)));

        InferenceOrchestratorService service = new InferenceOrchestratorService(bridge);

        service.validateL4(Map.of("l4_requirements", java.util.List.of())).block();

        verify(bridge).post(eq("/l4/validate"), eq(Map.of("l4_requirements", java.util.List.of())),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());
    }

    @Test
    void warmup_usesChatCompletionsMessagesContract() {
        PythonBridgeClient bridge = mock(PythonBridgeClient.class);
        when(bridge.post(eq("/chat/completions"), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(Mono.just(Map.of("content", "pong")));

        InferenceOrchestratorService service = new InferenceOrchestratorService(bridge);

        service.warmup().block();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bridge).post(eq("/chat/completions"), requestCaptor.capture(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        Map<String, Object> request = requestCaptor.getValue();
        assertThat(request).containsKeys("messages", "model", "thinking_enabled", "max_tokens");
        assertThat(request).doesNotContainKeys("prompt", "use_thinking_mode", "max_new_tokens");
        assertThat((List<?>) request.get("messages")).hasSize(2);
    }
}
