package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.AddBundleItemRequest;
import com.cloudys.requirementanalysis.dto.CreateBundleRequest;
import com.cloudys.requirementanalysis.dto.CreateContextRunRequest;
import com.cloudys.requirementanalysis.dto.CreateSpanLinkRequest;
import com.cloudys.requirementanalysis.dto.CreateSpanRequest;
import com.cloudys.requirementanalysis.service.ContextBuildService;

@RestController
@RequestMapping("/api/v2/analysis/context")
public class ContextController {

    private final ContextBuildService contextBuildService;

    public ContextController(ContextBuildService contextBuildService) {
        this.contextBuildService = contextBuildService;
    }

    @PostMapping("/spans")
    public ResponseEntity<Map<String, Object>> createSpan(@RequestBody CreateSpanRequest request) {
        return ResponseEntity.ok(contextBuildService.createSpan(request));
    }

    @GetMapping("/spans/{sessionId}")
    public ResponseEntity<Map<String, Object>> listSpans(@PathVariable String sessionId) {
        return ResponseEntity.ok(contextBuildService.listSpans(sessionId));
    }

    @PostMapping("/runs")
    public ResponseEntity<Map<String, Object>> createContextRun(@RequestBody CreateContextRunRequest request) {
        return ResponseEntity.ok(contextBuildService.createContextRun(request));
    }

    @GetMapping("/runs/{sessionId}")
    public ResponseEntity<Map<String, Object>> listContextRuns(@PathVariable String sessionId) {
        return ResponseEntity.ok(contextBuildService.listContextRuns(sessionId));
    }

    @PostMapping("/links")
    public ResponseEntity<Map<String, Object>> createSpanLink(@RequestBody CreateSpanLinkRequest request) {
        return ResponseEntity.ok(contextBuildService.createSpanLink(request));
    }

    @GetMapping("/links/{contextRunId}")
    public ResponseEntity<Map<String, Object>> listSpanLinks(@PathVariable String contextRunId) {
        return ResponseEntity.ok(contextBuildService.listSpanLinks(contextRunId));
    }

    @PostMapping("/bundles")
    public ResponseEntity<Map<String, Object>> createBundle(@RequestBody CreateBundleRequest request) {
        return ResponseEntity.ok(contextBuildService.createBundle(request));
    }

    @GetMapping("/bundles/{contextRunId}")
    public ResponseEntity<Map<String, Object>> listBundles(@PathVariable String contextRunId) {
        return ResponseEntity.ok(contextBuildService.listBundles(contextRunId));
    }

    @PostMapping("/bundles/{bundleId}/items")
    public ResponseEntity<Map<String, Object>> addBundleItem(@PathVariable String bundleId,
                                                              @RequestBody AddBundleItemRequest request) {
        return ResponseEntity.ok(contextBuildService.addBundleItem(bundleId, request));
    }
}
