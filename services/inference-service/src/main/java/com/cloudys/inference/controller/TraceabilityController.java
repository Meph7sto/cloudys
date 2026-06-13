package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.traceability.BatchTraceRelationRequest;
import com.cloudys.inference.dto.traceability.TraceRelationRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/traceability", "/inference/traceability"})
public class TraceabilityController {

    private final InferenceOrchestratorService orchestrator;

    public TraceabilityController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/relation")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeRelation(@Valid @RequestBody TraceRelationRequest request) {
        return orchestrator.analyzeRelation(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/batch-relation")
    public Mono<ResponseEntity<Map<String, Object>>> batchAnalyzeRelation(@Valid @RequestBody BatchTraceRelationRequest request) {
        return orchestrator.batchAnalyzeRelation(request)
                .map(ResponseEntity::ok);
    }
}
