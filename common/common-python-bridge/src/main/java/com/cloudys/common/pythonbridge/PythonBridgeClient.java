package com.cloudys.common.pythonbridge;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.cloudys.common.core.exception.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.channel.ChannelOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Java → Python HTTP 桥接客户端。
 * 封装 WebClient，通过 HTTP 调用 Python FastAPI 推理 sidecar。
 *
 * <p>使用方式（在 InferenceOrchestratorService 中）：
 * <pre>{@code
 *   Mono<Map<String, Object>> result = bridge.post("/chat/completions", request, MAP_TYPE);
 *   Flux<String> stream = bridge.postStream("/chat/stream", request, STRING_TYPE);
 * }</pre>
 */
public class PythonBridgeClient {

    private static final Logger log = LoggerFactory.getLogger(PythonBridgeClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PythonBridgeClient(PythonBridgeProperties props) {
        this(props, new ObjectMapper());
    }

    public PythonBridgeClient(PythonBridgeProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        var provider = ConnectionProvider.builder("python-bridge-pool")
                .maxConnections(props.getMaxConnections())
                .maxIdleTime(Duration.ofMillis(props.getMaxIdleTimeoutMs()))
                .build();

        var httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutMs() / 1000))
                        .addHandlerLast(new WriteTimeoutHandler(30)))
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(parseMaxInMemorySize(props.getMaxInMemorySize())))
                .build();

        log.info("PythonBridgeClient initialized — baseUrl={} poolSize={} readTimeoutMs={}",
                props.getBaseUrl(), props.getMaxConnections(), props.getReadTimeoutMs());
    }

    /**
     * 同步 POST 请求，返回反序列化后的对象。
     *
     * @param path         Python API 路径 (如 "/chat/completions")
     * @param body         请求体 (自动 JSON 序列化)
     * @param responseType 期望的响应类型
     * @param <T>          返回类型
     * @return Mono 响应
     */
    public <T> Mono<T> post(String path, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToMono(responseType)
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * 同步 POST 请求，返回 ParameterizedTypeReference 类型（泛型）。
     */
    public <T> Mono<T> post(String path, Object body, ParameterizedTypeReference<T> responseType) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToMono(responseType)
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * 流式 POST 请求（NDJSON → Flux）。
     * 读取 Python 返回的 application/x-ndjson 流，按行反序列化。
     *
     * @param path         Python API 路径
     * @param body         请求体
     * @param elementType  每行 JSON 对应的类型
     * @param <T>          元素类型
     * @return Flux 逐行响应
     */
    public <T> Flux<T> postStream(String path, Object body, Class<T> elementType) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    try {
                        return objectMapper.readValue(line, elementType);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse NDJSON line: {}", line, e);
                        // Return the raw line for debugging; callers should handle String gracefully
                        return null;
                    }
                })
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend stream error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * 流式 POST，返回原始字符串行（不做 JSON 反序列化）。
     * 适用于只需要原始 NDJSON 逐行转发的场景（SSE 代理）。
     */
    public Flux<String> postStreamRaw(String path, Object body) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToFlux(String.class)
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend stream error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * GET 请求。
     */
    public <T> Mono<T> get(String path, Class<T> responseType) {
        return webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToMono(responseType)
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * GET 请求，返回泛型类型。
     */
    public <T> Mono<T> get(String path, ParameterizedTypeReference<T> responseType) {
        return webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(detail -> Mono.error(
                                        new ErrorResponse(detail, resp.statusCode().value()))))
                .bodyToMono(responseType)
                .onErrorMap(WebClientResponseException.class, ex ->
                        new ErrorResponse("Python backend error: " + ex.getMessage(),
                                ex.getStatusCode().value()));
    }

    /**
     * 获取底层 WebClient（供高级用法，如 multipart 上传）。
     */
    public WebClient getWebClient() {
        return webClient;
    }

    /**
     * 获取 ObjectMapper。
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // --- private helpers ---

    private static int parseMaxInMemorySize(String size) {
        if (size == null || size.isBlank()) {
            return 16 * 1024 * 1024;
        }
        String s = size.toUpperCase().replace("B", "").trim();
        try {
            if (s.endsWith("M")) {
                return Integer.parseInt(s.replace("M", "")) * 1024 * 1024;
            } else if (s.endsWith("K")) {
                return Integer.parseInt(s.replace("K", "")) * 1024;
            } else if (s.endsWith("G")) {
                return Integer.parseInt(s.replace("G", "")) * 1024 * 1024 * 1024;
            } else {
                return Integer.parseInt(s);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid maxInMemorySize: {}, using default 16MB", size);
            return 16 * 1024 * 1024;
        }
    }
}
