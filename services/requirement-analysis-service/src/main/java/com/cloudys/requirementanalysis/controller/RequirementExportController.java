package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.service.RequirementExportService;

@RestController
@RequestMapping("/api/v2/analysis")
public class RequirementExportController {

    private final RequirementExportService requirementExportService;

    public RequirementExportController(RequirementExportService requirementExportService) {
        this.requirementExportService = requirementExportService;
    }

    @GetMapping("/sessions/{sessionId}/requirements-export")
    public ResponseEntity<Map<String, Object>> exportSessionRequirements(@PathVariable String sessionId) {
        return ResponseEntity.ok(requirementExportService.exportSessionRequirements(sessionId));
    }
}
