package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.ChangeAnalysisRequest;
import com.cloudys.requirementanalysis.service.RequirementChangeService;

@RestController
@RequestMapping("/api/v2/requirement-change")
public class RequirementChangeController {

    private final RequirementChangeService changeService;

    public RequirementChangeController(RequirementChangeService changeService) {
        this.changeService = changeService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody ChangeAnalysisRequest request) {
        return ResponseEntity.ok(changeService.analyzeChange(
                request.sessionId(), request.projectId(), request.changedReqIds()));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(changeService.getChangeHistory(sessionId));
    }
}
