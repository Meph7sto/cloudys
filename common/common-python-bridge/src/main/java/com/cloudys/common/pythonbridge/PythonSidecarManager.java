package com.cloudys.common.pythonbridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * 管理本地 Python sidecar 生命周期。
 * 仅在 python.bridge.manage-process=true 时生效。
 */
public class PythonSidecarManager {

    private static final Logger log = LoggerFactory.getLogger(PythonSidecarManager.class);

    private final PythonBridgeProperties properties;
    private final WebClient probeClient;
    private final AtomicBoolean startedByManager = new AtomicBoolean(false);
    private volatile Process process;

    public PythonSidecarManager(PythonBridgeProperties properties) {
        this.properties = properties;
        this.probeClient = WebClient.builder()
                .baseUrl(resolveBaseUrl(properties))
                .build();
    }

    public synchronized void ensureStarted() {
        if (!properties.isManageProcess()) {
            return;
        }
        if (isProcessAlive()) {
            return;
        }
        ProcessBuilder builder = new ProcessBuilder(buildCommand(properties));
        builder.redirectErrorStream(true);
        if (!properties.getWorkingDirectory().isBlank()) {
            builder.directory(Path.of(properties.getWorkingDirectory()).toFile());
        }
        applyEnvironment(builder, properties.getEnvironment());
        try {
            process = builder.start();
            startedByManager.set(true);
            startLogPump(process);
            log.info("Started managed Python sidecar pid={} baseUrl={}", process.pid(), resolveBaseUrl(properties));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start Python sidecar process", ex);
        }
    }

    public Mono<Map<String, Object>> awaitReady() {
        return Mono.fromCallable(() -> waitUntilReady());
    }

    public Mono<Map<String, Object>> healthSnapshot() {
        return probeClient.get()
                .uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public boolean isManaged() {
        return properties.isManageProcess();
    }

    public boolean isProcessAlive() {
        return process != null && process.isAlive();
    }

    public Long pid() {
        return process != null ? process.pid() : null;
    }

    public String baseUrl() {
        return resolveBaseUrl(properties);
    }

    public synchronized void shutdown() {
        if (!startedByManager.get() || process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private Map<String, Object> waitUntilReady() {
        Instant deadline = Instant.now().plusMillis(properties.getStartupTimeoutMs());
        Throwable lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                Map<String, Object> body = healthSnapshot().block(Duration.ofSeconds(5));
                if (body != null) {
                    return body;
                }
            } catch (Exception ex) {
                lastFailure = ex;
            }
            if (process != null && !process.isAlive()) {
                throw new IllegalStateException("Python sidecar exited before becoming ready");
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Python sidecar", ex);
            }
        }
        throw new IllegalStateException("Timed out waiting for Python sidecar readiness", lastFailure);
    }

    private static String resolveBaseUrl(PythonBridgeProperties properties) {
        String configured = properties.getBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "http://" + properties.getHost() + ":" + properties.getPort();
    }

    private static List<String> buildCommand(PythonBridgeProperties properties) {
        return List.of(
                properties.getPythonExecutable(),
                "-m",
                "uvicorn",
                properties.getAppModule(),
                "--host",
                properties.getHost(),
                "--port",
                Integer.toString(properties.getPort())
        );
    }

    private static void applyEnvironment(ProcessBuilder builder, List<String> entries) {
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank() || !entry.contains("=")) {
                continue;
            }
            int idx = entry.indexOf('=');
            builder.environment().put(entry.substring(0, idx), entry.substring(idx + 1));
        }
    }

    private static void startLogPump(Process process) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[python-sidecar] {}", line);
                }
            } catch (IOException ex) {
                log.debug("Python sidecar log pump stopped: {}", ex.getMessage());
            }
        }, "python-sidecar-log-pump");
        logThread.setDaemon(true);
        logThread.start();
    }
}
