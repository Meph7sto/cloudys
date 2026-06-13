package com.cloudys.auth.controller;

import com.cloudys.auth.dto.request.CreateBaselineRequest;
import com.cloudys.auth.service.BaselineService;
import com.cloudys.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/permission")
public class BaselineController {

    private final BaselineService baselineService;

    public BaselineController(BaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @PostMapping("/projects/{projectId}/baselines")
    public ResponseEntity<Map<String, Object>> create(@PathVariable String projectId,
                                                       @Valid @RequestBody CreateBaselineRequest request) {
        return ResponseEntity.ok(baselineService.create(
                projectId, request.version(), request.locked(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/projects/{projectId}/baselines")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String projectId) {
        return ResponseEntity.ok(baselineService.listBaselines(projectId));
    }

    @PostMapping("/baselines/{id}/lock")
    public ResponseEntity<Map<String, Object>> lock(@PathVariable Long id) {
        return ResponseEntity.ok(baselineService.lockBaseline(id));
    }

    @PostMapping("/baselines/{id}/unlock")
    public ResponseEntity<Map<String, Object>> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(baselineService.unlockBaseline(id));
    }

}
