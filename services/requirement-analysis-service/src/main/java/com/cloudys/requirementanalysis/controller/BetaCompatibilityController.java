package com.cloudys.requirementanalysis.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cloudys.requirementanalysis.service.BetaCompatibilityService;

@RestController
@RequestMapping("/api/v2")
public class BetaCompatibilityController {

    private final BetaCompatibilityService betaCompatibilityService;

    public BetaCompatibilityController(BetaCompatibilityService betaCompatibilityService) {
        this.betaCompatibilityService = betaCompatibilityService;
    }

    @GetMapping("/analysis/sample_transcript")
    public ResponseEntity<String> sampleTranscript() {
        return ResponseEntity.ok(betaCompatibilityService.sampleTranscript());
    }

    @PostMapping(value = "/analysis/ingest_transcript/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ingestTranscript(@RequestBody Map<String, Object> payload) {
        return betaCompatibilityService.ingestTranscriptStream(
                String.valueOf(payload.getOrDefault("session_id", "")),
                String.valueOf(payload.getOrDefault("transcript_text", ""))
        );
    }

    @PostMapping(value = "/analysis/build_context/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter buildContext(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) payload.getOrDefault("options", Map.of());
        return betaCompatibilityService.buildContextStream(String.valueOf(payload.getOrDefault("session_id", "")), options);
    }

    @GetMapping("/analysis/context_runs")
    public ResponseEntity<Map<String, Object>> listContextRuns(@RequestParam(name = "session_id") String sessionId) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.listContextRuns(sessionId).get("runs")));
    }

    @GetMapping("/analysis/spans/{sessionId}")
    public ResponseEntity<Map<String, Object>> listSpans(@PathVariable String sessionId) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.listSpans(sessionId)));
    }

    @GetMapping("/analysis/span_links/{contextRunId}")
    public ResponseEntity<Map<String, Object>> listSpanLinks(@PathVariable String contextRunId) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.listSpanLinks(contextRunId)));
    }

    @PostMapping("/requirements/extract_l123")
    public ResponseEntity<Map<String, Object>> extractL123(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(betaCompatibilityService.extractL123(payload));
    }

    @PostMapping(value = "/requirements/extract_l123/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter extractL123Stream(@RequestBody Map<String, Object> payload) {
        return betaCompatibilityService.extractL123Stream(payload);
    }

    @GetMapping("/requirements/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> listRequirementsBySession(@PathVariable String sessionId,
                                                                          @RequestParam(name = "level", required = false) String level,
                                                                          @RequestParam(name = "page", defaultValue = "1") int page,
                                                                          @RequestParam(name = "per_page", defaultValue = "50") int perPage) {
        return ResponseEntity.ok(betaCompatibilityService.listRequirementsBySession(sessionId, level, page, perPage));
    }

    @GetMapping("/requirements/stats")
    public ResponseEntity<Map<String, Object>> requirementStats(@RequestParam(name = "session_id", required = false) String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.requirementsStats(sessionId));
    }

    @GetMapping("/requirements/l4/{sessionId}")
    public ResponseEntity<Map<String, Object>> getL4BySession(@PathVariable String sessionId,
                                                               @RequestParam(name = "page", defaultValue = "1") int page,
                                                               @RequestParam(name = "per_page", defaultValue = "100") int perPage) {
        return ResponseEntity.ok(betaCompatibilityService.getL4BySession(sessionId, page, perPage));
    }

    @GetMapping("/requirements/l4/{sessionId}/exists")
    public ResponseEntity<Map<String, Object>> l4Exists(@PathVariable String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.checkL4Exists(sessionId));
    }

    @DeleteMapping("/requirements/l4/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearL4(@PathVariable String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.clearL4(sessionId));
    }

    @PostMapping("/requirements/l4/generate")
    public ResponseEntity<Map<String, Object>> generateL4(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(betaCompatibilityService.generateL4(payload));
    }

    @GetMapping("/classification/latest")
    public ResponseEntity<Map<String, Object>> classificationLatest(@RequestParam(name = "session_id") String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.classificationLatest(sessionId));
    }

    @PostMapping("/conflict/analyze")
    public ResponseEntity<Map<String, Object>> conflictAnalyze(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(betaCompatibilityService.conflictAnalyze(payload));
    }

    @GetMapping("/conflict/latest")
    public ResponseEntity<Map<String, Object>> conflictLatest(@RequestParam(name = "session_id") String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.conflictLatest(sessionId));
    }

    @PostMapping("/trace-by-mapping")
    public ResponseEntity<Map<String, Object>> traceByMapping(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(betaCompatibilityService.traceByMapping(payload));
    }

    @GetMapping("/trace/latest")
    public ResponseEntity<Map<String, Object>> traceLatest(@RequestParam(name = "session_id") String sessionId) {
        return ResponseEntity.ok(betaCompatibilityService.traceLatest(sessionId));
    }

    @PostMapping("/analysis/runs")
    public ResponseEntity<Map<String, Object>> saveRun(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.saveRun(payload)));
    }

    @GetMapping("/analysis/runs")
    public ResponseEntity<Map<String, Object>> listRuns(@RequestParam(name = "session_id") String sessionId,
                                                         @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.listRuns(sessionId, limit)));
    }

    @GetMapping("/analysis/runs/latest")
    public ResponseEntity<Map<String, Object>> latestRun(@RequestParam(name = "session_id") String sessionId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", betaCompatibilityService.latestRun(sessionId));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/analysis/runs/{analysisRunId}")
    public ResponseEntity<Map<String, Object>> getRun(@PathVariable String analysisRunId) {
        return ResponseEntity.ok(Map.of("data", betaCompatibilityService.getRun(analysisRunId)));
    }
}
