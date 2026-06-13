package com.cloudys.inference.controller;

import java.util.Map;

import com.cloudys.inference.service.InferenceOrchestratorService;
import com.cloudys.inference.service.InferenceRuntimeCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private WebTestClient webTestClient;
    private InferenceOrchestratorService orchestrator;
    private InferenceRuntimeCoordinator runtimeCoordinator;

    @BeforeEach
    void setUp() {
        orchestrator = mock(InferenceOrchestratorService.class);
        runtimeCoordinator = mock(InferenceRuntimeCoordinator.class);
        when(runtimeCoordinator.snapshot()).thenReturn(Map.of("startup_state", "ready"));
        webTestClient = WebTestClient.bindToController(new HealthController(orchestrator, runtimeCoordinator)).build();
    }

    @Test
    void health_pythonBackendUp_returnsOk() {
        when(orchestrator.pythonHealth())
                .thenReturn(Mono.just(Map.of("model_loaded", true)));

        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("inference_service_ok")
                .jsonPath("$.python_backend.model_loaded").isEqualTo(true)
                .jsonPath("$.runtime.startup_state").isEqualTo("ready");
    }

    @Test
    void health_pythonBackendDown_returnsDegraded() {
        when(orchestrator.pythonHealth())
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("inference_service_degraded")
                .jsonPath("$.python_backend.reachable").isEqualTo(false)
                .jsonPath("$.runtime.startup_state").isEqualTo("ready");
    }
}
