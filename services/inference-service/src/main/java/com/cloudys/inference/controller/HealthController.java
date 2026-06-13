package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.service.InferenceOrchestratorService;
import com.cloudys.inference.service.InferenceRuntimeCoordinator;

import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    private final InferenceOrchestratorService orchestrator;
    private final InferenceRuntimeCoordinator runtimeCoordinator;

    public HealthController(InferenceOrchestratorService orchestrator,
                            InferenceRuntimeCoordinator runtimeCoordinator) {
        this.orchestrator = orchestrator;
        this.runtimeCoordinator = runtimeCoordinator;
    }

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return orchestrator.pythonHealth()
                .map(pyStatus -> Map.<String, Object>of(
                        "status", "inference_service_ok",
                        "python_backend", pyStatus,
                        "runtime", runtimeCoordinator.snapshot()
                ))
                .onErrorResume(ex -> Mono.just(Map.<String, Object>of(
                        "status", "inference_service_degraded",
                        "python_backend", Map.of("reachable", false, "error", ex.getMessage()),
                        "runtime", runtimeCoordinator.snapshot()
                )));
    }
}
