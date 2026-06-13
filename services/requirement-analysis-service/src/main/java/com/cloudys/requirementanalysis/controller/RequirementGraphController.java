package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.InvalidateRelationsRequest;
import com.cloudys.requirementanalysis.service.RequirementGraphService;

@RestController
@RequestMapping("/api/v2/requirement-graph")
public class RequirementGraphController {

    private final RequirementGraphService graphService;

    public RequirementGraphController(RequirementGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/relations")
    public ResponseEntity<Map<String, Object>> searchRelations(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String relationMode,
            @RequestParam(defaultValue = "true") Boolean activeOnly) {
        return ResponseEntity.ok(graphService.searchRelations(projectId, sessionId, relationMode, activeOnly));
    }

    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Map<String, Object>> getSnapshot(@PathVariable String snapshotId) {
        return ResponseEntity.ok(graphService.getSnapshot(snapshotId));
    }

    @PostMapping("/invalidate")
    public ResponseEntity<Map<String, Object>> invalidate(@RequestBody InvalidateRelationsRequest request) {
        return ResponseEntity.ok(graphService.invalidateRelations(
                request.snapshotId(), request.relationIds(), request.reason()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String relationMode) {
        return ResponseEntity.ok(graphService.getStats(projectId, sessionId, relationMode));
    }
}
