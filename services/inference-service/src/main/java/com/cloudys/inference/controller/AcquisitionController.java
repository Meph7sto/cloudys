package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.acquisition.ExtractionRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/acquisition", "/inference/acquisition"})
public class AcquisitionController {

    private final InferenceOrchestratorService orchestrator;

    public AcquisitionController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/extract")
    public Mono<ResponseEntity<Map<String, Object>>> extract(@Valid @RequestBody ExtractionRequest request) {
        return orchestrator.extractRequirements(request)
                .map(ResponseEntity::ok);
    }
}
