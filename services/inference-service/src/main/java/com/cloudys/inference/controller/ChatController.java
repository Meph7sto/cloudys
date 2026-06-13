package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.chat.ChatCompletionRequest;
import com.cloudys.inference.dto.chat.ChatCompletionWithToolsRequest;
import com.cloudys.inference.dto.chat.ChatRequest;
import com.cloudys.inference.dto.chat.ContextCompletionRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/chat", "/inference/chat"})
public class ChatController {

    private final InferenceOrchestratorService orchestrator;

    public ChatController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/stream", produces = "application/x-ndjson")
    public Mono<ResponseEntity<Flux<String>>> streamChat(@Valid @RequestBody ChatRequest request) {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(orchestrator.streamChat(request)));
    }

    @PostMapping("/completions")
    public Mono<ResponseEntity<Map<String, Object>>> completions(@Valid @RequestBody ChatCompletionRequest request) {
        return orchestrator.chatCompletions(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/completions/tools")
    public Mono<ResponseEntity<Map<String, Object>>> completionsTools(@Valid @RequestBody ChatCompletionWithToolsRequest request) {
        return orchestrator.chatCompletionsTools(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/completions/tools/stream", produces = "application/x-ndjson")
    public Mono<ResponseEntity<Flux<String>>> completionsToolsStream(@Valid @RequestBody ChatCompletionWithToolsRequest request) {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(orchestrator.chatCompletionsToolsStream(request)));
    }

    @PostMapping("/context-completion")
    public Mono<ResponseEntity<Map<String, Object>>> contextCompletion(@Valid @RequestBody ContextCompletionRequest request) {
        return orchestrator.contextCompletion(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/context-completion/stream", produces = "application/x-ndjson")
    public Mono<ResponseEntity<Flux<String>>> contextCompletionStream(@Valid @RequestBody ContextCompletionRequest request) {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(orchestrator.contextCompletionStream(request)));
    }
}
