package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.kb.KBSearchRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/kb", "/inference/kb"})
public class KBController {

    private final InferenceOrchestratorService orchestrator;

    public KBController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> search(@Valid @RequestBody KBSearchRequest request) {
        return orchestrator.searchKb(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> status() {
        return orchestrator.kbStatus()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/rebuild")
    public Mono<ResponseEntity<Map<String, Object>>> rebuild() {
        return orchestrator.rebuildKb()
                .map(ResponseEntity::ok);
    }
}
