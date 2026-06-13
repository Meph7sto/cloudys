package com.cloudys.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.cloudys.common.pythonbridge.PythonBridgeProperties;
import com.cloudys.common.pythonbridge.PythonSidecarManager;

import reactor.core.publisher.Mono;

class InferenceRuntimeCoordinatorTest {

    @Test
    void run_whenWarmupDisabledAndUnmanaged_marksSkipped() {
        PythonBridgeProperties properties = new PythonBridgeProperties();
        properties.setManageProcess(false);
        properties.setWarmupEnabled(false);

        PythonSidecarManager sidecarManager = mock(PythonSidecarManager.class);
        when(sidecarManager.baseUrl()).thenReturn("http://localhost:8000");
        when(sidecarManager.isProcessAlive()).thenReturn(false);

        InferenceOrchestratorService orchestrator = mock(InferenceOrchestratorService.class);

        InferenceRuntimeCoordinator coordinator =
                new InferenceRuntimeCoordinator(properties, sidecarManager, orchestrator);

        coordinator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(coordinator.snapshot())
                .containsEntry("startup_state", "skipped")
                .containsEntry("managed_process", false)
                .containsEntry("warmup_enabled", false);
        verify(orchestrator, never()).pythonHealth();
        verify(sidecarManager, never()).ensureStarted();
    }

    @Test
    void run_whenManagedProcessEnabled_waitsForSidecar() {
        PythonBridgeProperties properties = new PythonBridgeProperties();
        properties.setManageProcess(true);
        properties.setWarmupEnabled(true);
        properties.setStartupTimeoutMs(5000);

        PythonSidecarManager sidecarManager = mock(PythonSidecarManager.class);
        when(sidecarManager.baseUrl()).thenReturn("http://127.0.0.1:8000");
        when(sidecarManager.isProcessAlive()).thenReturn(true);
        when(sidecarManager.pid()).thenReturn(1234L);
        when(sidecarManager.awaitReady()).thenReturn(Mono.just(Map.of("status", "ok")));

        InferenceOrchestratorService orchestrator = mock(InferenceOrchestratorService.class);
        when(orchestrator.warmup()).thenReturn(Mono.just(Map.of("content", "pong")));

        InferenceRuntimeCoordinator coordinator =
                new InferenceRuntimeCoordinator(properties, sidecarManager, orchestrator);

        coordinator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(coordinator.snapshot())
                .containsEntry("startup_state", "ready")
                .containsEntry("process_alive", true)
                .containsEntry("pid", 1234L);
        assertThat(coordinator.snapshot()).containsKey("warmup_result");
        verify(sidecarManager).ensureStarted();
        verify(sidecarManager).awaitReady();
        verify(orchestrator, never()).pythonHealth();
        verify(orchestrator).warmup();
    }

    @Test
    void run_whenWarmupFails_keepsServiceReadyWithWarmupErrorRecorded() {
        PythonBridgeProperties properties = new PythonBridgeProperties();
        properties.setManageProcess(false);
        properties.setWarmupEnabled(true);
        properties.setStartupTimeoutMs(5000);

        PythonSidecarManager sidecarManager = mock(PythonSidecarManager.class);
        when(sidecarManager.baseUrl()).thenReturn("http://127.0.0.1:8000");
        when(sidecarManager.isProcessAlive()).thenReturn(false);

        InferenceOrchestratorService orchestrator = mock(InferenceOrchestratorService.class);
        when(orchestrator.pythonHealth()).thenReturn(Mono.just(Map.of("status", "ok")));
        when(orchestrator.warmup()).thenReturn(Mono.error(new RuntimeException("warmup failed")));

        InferenceRuntimeCoordinator coordinator =
                new InferenceRuntimeCoordinator(properties, sidecarManager, orchestrator);

        coordinator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(coordinator.snapshot())
                .containsEntry("startup_state", "ready")
                .containsKey("warmup_result");
        @SuppressWarnings("unchecked")
        Map<String, Object> warmupResult = (Map<String, Object>) coordinator.snapshot().get("warmup_result");
        assertThat(warmupResult)
                .containsEntry("status", "failed")
                .containsKey("error");
        verify(orchestrator).pythonHealth();
        verify(orchestrator).warmup();
    }
}
