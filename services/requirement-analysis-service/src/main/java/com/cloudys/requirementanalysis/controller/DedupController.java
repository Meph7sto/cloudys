package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.DedupRequest;
import com.cloudys.requirementanalysis.service.DedupService;

@RestController
@RequestMapping("/api/v2/dedup")
public class DedupController {

    private final DedupService dedupService;

    public DedupController(DedupService dedupService) {
        this.dedupService = dedupService;
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(@RequestBody DedupRequest request) {
        return ResponseEntity.ok(dedupService.detectDuplicates(request.sessionId(), request.requirementTexts()));
    }

    @GetMapping("/sessions/{sessionId}/results")
    public ResponseEntity<Map<String, Object>> getResults(@PathVariable String sessionId) {
        return ResponseEntity.ok(dedupService.getDedupResults(sessionId));
    }
}
