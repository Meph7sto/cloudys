package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.conflict.ConflictCheckRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/conflict", "/inference/conflict"})
public class ConflictController {

    private final InferenceOrchestratorService orchestrator;

    public ConflictController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/check")
    public Mono<ResponseEntity<Map<String, Object>>> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return orchestrator.checkConflict(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/stream-check", produces = "application/x-ndjson")
    public Mono<ResponseEntity<Flux<String>>> streamCheckConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(orchestrator.streamCheckConflict(request)));
    }
}
