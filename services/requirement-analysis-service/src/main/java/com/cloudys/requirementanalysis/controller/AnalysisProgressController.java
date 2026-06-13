package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cloudys.requirementanalysis.dto.AnalysisRunRequest;
import com.cloudys.requirementanalysis.service.AnalysisOrchestratorService;
import com.cloudys.requirementanalysis.service.ProgressEmitterService;

@RestController
@RequestMapping("/api/v2/analysis")
public class AnalysisProgressController {

    private final AnalysisOrchestratorService orchestrator;
    private final ProgressEmitterService progressEmitter;

    public AnalysisProgressController(AnalysisOrchestratorService orchestrator,
                                       ProgressEmitterService progressEmitter) {
        this.orchestrator = orchestrator;
        this.progressEmitter = progressEmitter;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> startAnalysis(@RequestBody AnalysisRunRequest request) {
        Map<String, Object> result = orchestrator.startAnalysis(
                request.sessionId(), request.projectId(), request.contextRunId());
        String runId = (String) result.get("analysis_run_id");
        orchestrator.executeAnalysis(
                runId,
                request.sessionId(),
                request.projectId(),
                request.contextRunId(),
                request.autoImport(),
                request.mappingMode()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/progress/{analysisRunId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String analysisRunId) {
        return progressEmitter.createEmitter(analysisRunId);
    }
}
