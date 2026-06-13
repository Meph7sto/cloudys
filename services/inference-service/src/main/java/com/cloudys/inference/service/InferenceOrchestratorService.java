package com.cloudys.inference.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;

import com.cloudys.common.pythonbridge.PythonBridgeClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 推理编排服务。
 * 将控制器请求转发到 Python FastAPI 推理 sidecar，处理路径映射和响应转换。
 *
 * <p>所有方法均返回 Mono/Flux，与 WebFlux 响应式控制器兼容。
 */
@Service
public class InferenceOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(InferenceOrchestratorService.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final PythonBridgeClient bridge;

    public InferenceOrchestratorService(PythonBridgeClient bridge) {
        this.bridge = bridge;
    }

    // ========================
    // Chat
    // ========================

    /** POST /chat/stream → NDJSON Flux */
    public Flux<String> streamChat(Object request) {
        return bridge.postStreamRaw("/chat/stream", request);
    }

    /** POST /chat/completions → Map */
    public Mono<Map<String, Object>> chatCompletions(Object request) {
        return bridge.post("/chat/completions", request, MAP_TYPE);
    }

    /** POST /chat/completions/tools → Map */
    public Mono<Map<String, Object>> chatCompletionsTools(Object request) {
        return bridge.post("/chat/completions/tools", request, MAP_TYPE);
    }

    /** POST /chat/completions/tools/stream → NDJSON Flux */
    public Flux<String> chatCompletionsToolsStream(Object request) {
        return bridge.postStreamRaw("/chat/completions/tools/stream", request);
    }

    /** POST /chat/context-completion → Map */
    public Mono<Map<String, Object>> contextCompletion(Object request) {
        return bridge.post("/chat/context-completion", request, MAP_TYPE);
    }

    /** POST /chat/context-completion/stream → NDJSON Flux */
    public Flux<String> contextCompletionStream(Object request) {
        return bridge.postStreamRaw("/chat/context-completion/stream", request);
    }

    // ========================
    // Classification
    // ========================

    /** POST /classification/predict-texts → Map */
    public Mono<Map<String, Object>> classifyTexts(Object request) {
        return bridge.post("/classification/predict-texts", request, MAP_TYPE);
    }

    /** POST /classification/predict-csv (multipart) → byte[] */
    public Mono<byte[]> classifyCsv(FilePart file) {
        return bridge.getWebClient().post()
                .uri("/classification/predict-csv")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file))
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(data -> log.info("CSV classification complete, {} bytes", data != null ? data.length : 0));
    }

    // ========================
    // Conflict
    // ========================

    /** POST /conflict/check → Map */
    public Mono<Map<String, Object>> checkConflict(Object request) {
        return bridge.post("/conflict/check", request, MAP_TYPE);
    }

    /** POST /conflict/stream_check → NDJSON Flux */
    public Flux<String> streamCheckConflict(Object request) {
        return bridge.postStreamRaw("/conflict/stream-check", request);
    }

    // ========================
    // Traceability
    // ========================

    /** POST /traceability/relation → Map */
    public Mono<Map<String, Object>> analyzeRelation(Object request) {
        return bridge.post("/traceability/relation", request, MAP_TYPE);
    }

    /** POST /traceability/batch-relation → Map */
    public Mono<Map<String, Object>> batchAnalyzeRelation(Object request) {
        return bridge.post("/traceability/batch-relation", request, MAP_TYPE);
    }

    // ========================
    // Knowledge Base
    // ========================

    /** POST /kb/search → Map */
    public Mono<Map<String, Object>> searchKb(Object request) {
        return bridge.post("/kb/search", request, MAP_TYPE);
    }

    /** GET /kb/status → Map */
    public Mono<Map<String, Object>> kbStatus() {
        return bridge.get("/kb/status", MAP_TYPE);
    }

    /** POST /kb/rebuild → Map */
    public Mono<Map<String, Object>> rebuildKb() {
        return bridge.post("/kb/rebuild", Map.of(), MAP_TYPE);
    }

    // ========================
    // L4 Generation & Validation
    // ========================

    /** POST /l4/generate → Map */
    public Mono<Map<String, Object>> generateL4(Object request) {
        return bridge.post("/l4/generate", request, MAP_TYPE);
    }

    /** POST /l4/validation/validate → Map */
    public Mono<Map<String, Object>> validateL4(Object request) {
        return bridge.post("/l4/validate", request, MAP_TYPE);
    }

    // ========================
    // Requirements Acquisition
    // ========================

    /** POST /acquisition/extract → Map */
    public Mono<Map<String, Object>> extractRequirements(Object request) {
        return bridge.post("/acquisition/extract", request, MAP_TYPE);
    }

    // ========================
    // Health
    // ========================

    /** GET /health → Python 健康状态 */
    public Mono<Map<String, Object>> pythonHealth() {
        return bridge.get("/health", MAP_TYPE);
    }

    public Mono<Map<String, Object>> warmup() {
        Map<String, Object> request = Map.of(
                "prompt", "ping",
                "system_prompt", "Warm up the model and return a short acknowledgement.",
                "max_new_tokens", 8,
                "use_thinking_mode", false,
                "use_async_client", false
        );
        return bridge.post("/chat/completions", request, MAP_TYPE);
    }
}
