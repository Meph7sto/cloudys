package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.l4.GenerateL4Request;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/l4", "/inference/l4"})
public class L4Controller {

    private final InferenceOrchestratorService orchestrator;

    public L4Controller(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generate(@Valid @RequestBody GenerateL4Request request) {
        return orchestrator.generateL4(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validate(@Valid @RequestBody Map<String, Object> request) {
        return orchestrator.validateL4(request)
                .map(ResponseEntity::ok);
    }
}
