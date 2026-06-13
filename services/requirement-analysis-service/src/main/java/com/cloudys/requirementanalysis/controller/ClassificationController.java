package com.cloudys.requirementanalysis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.ClassifyRequest;
import com.cloudys.requirementanalysis.service.ClassificationService;

@RestController
@RequestMapping("/api/v2/classification")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classify(@RequestBody ClassifyRequest request) {
        return ResponseEntity.ok(classificationService.classifyRequirements(request.sessionId(), request.requirements()));
    }

    @PostMapping("/predict-texts")
    public ResponseEntity<Map<String, Object>> predictTexts(@RequestBody Map<String, Object> request) {
        String sessionId = String.valueOf(request.getOrDefault("session_id", request.getOrDefault("sessionId", "")));
        @SuppressWarnings("unchecked")
        List<String> requirements = (List<String>) request.getOrDefault("requirements", List.of());
        return ResponseEntity.ok(classificationService.classifyRequirements(sessionId, requirements));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ResponseEntity<Map<String, Object>> getResults(@PathVariable String sessionId) {
        return ResponseEntity.ok(classificationService.getClassificationResults(sessionId));
    }
}
