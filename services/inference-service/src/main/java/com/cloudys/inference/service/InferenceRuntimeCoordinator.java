package com.cloudys.inference.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.cloudys.common.pythonbridge.PythonBridgeProperties;
import com.cloudys.common.pythonbridge.PythonSidecarManager;

import reactor.core.publisher.Mono;

/**
 * 推理服务启动协调器。
 * 负责本地 sidecar 进程拉起、远端就绪探测以及启动期预热状态记录。
 */
@Component
public class InferenceRuntimeCoordinator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InferenceRuntimeCoordinator.class);

    private final PythonBridgeProperties bridgeProperties;
    private final PythonSidecarManager sidecarManager;
    private final InferenceOrchestratorService orchestrator;
    private final AtomicReference<String> startupState = new AtomicReference<>("pending");
    private final AtomicReference<Map<String, Object>> startupHealth = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<String, Object>> warmupResult = new AtomicReference<>(Map.of());
    private volatile Instant readyAt;

    public InferenceRuntimeCoordinator(
            PythonBridgeProperties bridgeProperties,
            PythonSidecarManager sidecarManager,
            InferenceOrchestratorService orchestrator) {
        this.bridgeProperties = bridgeProperties;
        this.sidecarManager = sidecarManager;
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        startupState.set("starting");
        try {
            if (bridgeProperties.isManageProcess()) {
                sidecarManager.ensureStarted();
            }

            if (bridgeProperties.isManageProcess() || bridgeProperties.isWarmupEnabled()) {
                Map<String, Object> health = awaitPythonBackend();
                startupHealth.set(health);
                if (bridgeProperties.isWarmupEnabled()) {
                    warmupResult.set(runWarmup());
                }
                readyAt = Instant.now();
                startupState.set("ready");
                log.info("Inference runtime ready: baseUrl={} managed={} processAlive={}",
                        sidecarManager.baseUrl(),
                        bridgeProperties.isManageProcess(),
                        sidecarManager.isProcessAlive());
                return;
            }

            startupState.set("skipped");
            log.info("Inference runtime startup probe skipped: managed={} warmupEnabled={}",
                    bridgeProperties.isManageProcess(),
                    bridgeProperties.isWarmupEnabled());
        } catch (Exception ex) {
            startupState.set("failed");
            log.error("Inference runtime bootstrap failed", ex);
            throw ex;
        }
    }

    public Map<String, Object> snapshot() {
        LinkedHashMap<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("base_url", sidecarManager.baseUrl());
        runtime.put("managed_process", bridgeProperties.isManageProcess());
        runtime.put("warmup_enabled", bridgeProperties.isWarmupEnabled());
        runtime.put("process_alive", sidecarManager.isProcessAlive());
        if (sidecarManager.pid() != null) {
            runtime.put("pid", sidecarManager.pid());
        }
        runtime.put("startup_state", startupState.get());
        if (!startupHealth.get().isEmpty()) {
            runtime.put("startup_health", startupHealth.get());
        }
        if (!warmupResult.get().isEmpty()) {
            runtime.put("warmup_result", warmupResult.get());
        }
        if (readyAt != null) {
            runtime.put("ready_at", readyAt.toString());
        }
        return runtime;
    }

    private Map<String, Object> awaitPythonBackend() {
        if (bridgeProperties.isManageProcess()) {
            return sidecarManager.awaitReady()
                    .block(Duration.ofMillis(bridgeProperties.getStartupTimeoutMs()));
        }
        return orchestrator.pythonHealth()
                .block(Duration.ofMillis(bridgeProperties.getStartupTimeoutMs()));
    }

    private Map<String, Object> runWarmup() {
        return orchestrator.warmup()
                .map(response -> Map.<String, Object>of(
                        "status", "ok",
                        "response", response
                ))
                .onErrorResume(ex -> {
                    log.warn("Inference runtime warmup failed: {}", ex.getMessage());
                    return Mono.just(Map.<String, Object>of(
                            "status", "failed",
                            "error", ex.getMessage()
                    ));
                })
                .block(Duration.ofMillis(bridgeProperties.getStartupTimeoutMs()));
    }
}
