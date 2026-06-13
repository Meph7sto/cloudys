package com.cloudys.project.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.project.service.ManageCompatibilityService;

@RestController
@RequestMapping("/api/v2/manage")
public class ManageCompatibilityController {

    private final ManageCompatibilityService manageCompatibilityService;

    public ManageCompatibilityController(ManageCompatibilityService manageCompatibilityService) {
        this.manageCompatibilityService = manageCompatibilityService;
    }

    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<Map<String, Object>> listRequirements(@PathVariable String projectId,
                                                                 @RequestParam(name = "tree", defaultValue = "false") boolean tree) {
        return ResponseEntity.ok(manageCompatibilityService.listRequirements(projectId, tree));
    }

    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<Map<String, Object>> createRequirement(@PathVariable String projectId,
                                                                  @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.createRequirement(projectId, request));
    }

    @PatchMapping("/requirements/{reqId}")
    public ResponseEntity<Map<String, Object>> updateRequirement(@PathVariable String reqId,
                                                                  @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.updateRequirement(reqId, request));
    }

    @PostMapping("/requirements/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.bulkUpdateRequirementStatus(request));
    }

    @GetMapping("/projects/{projectId}/defects")
    public ResponseEntity<Map<String, Object>> listDefects(@PathVariable String projectId) {
        return ResponseEntity.ok(manageCompatibilityService.listDefects(projectId));
    }

    @GetMapping("/requirements/{reqId}/defects")
    public ResponseEntity<Map<String, Object>> listRequirementDefects(@PathVariable String reqId) {
        return ResponseEntity.ok(manageCompatibilityService.listRequirementDefects(reqId));
    }

    @PostMapping("/projects/{projectId}/defects")
    public ResponseEntity<Map<String, Object>> createDefect(@PathVariable String projectId,
                                                             @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.createDefect(projectId, request));
    }

    @PatchMapping("/defects/{defectId}")
    public ResponseEntity<Map<String, Object>> updateDefect(@PathVariable String defectId,
                                                             @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.updateDefect(defectId, request));
    }

    @DeleteMapping("/defects/{defectId}")
    public ResponseEntity<Map<String, Object>> deleteDefect(@PathVariable String defectId) {
        return ResponseEntity.ok(manageCompatibilityService.deleteDefect(defectId));
    }

    @PostMapping("/requirements/{reqId}/move")
    public ResponseEntity<Map<String, Object>> moveRequirement(@PathVariable String reqId,
                                                                 @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.moveRequirement(reqId, request));
    }

    @DeleteMapping("/requirements/{reqId}")
    public ResponseEntity<Map<String, Object>> deleteRequirement(@PathVariable String reqId,
                                                                  @RequestParam(name = "cascade", defaultValue = "false") boolean cascade) {
        return ResponseEntity.ok(manageCompatibilityService.deleteRequirement(reqId, cascade));
    }

    @PostMapping("/projects/{projectId}/requirements/import")
    public ResponseEntity<Map<String, Object>> importRequirements(@PathVariable String projectId,
                                                                    @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(manageCompatibilityService.importRequirements(projectId, request));
    }

    @GetMapping("/projects/{projectId}/audits")
    public ResponseEntity<Map<String, Object>> listAudits(@PathVariable String projectId,
                                                           @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return ResponseEntity.ok(manageCompatibilityService.listAudits(projectId, limit));
    }

    @GetMapping("/projects/{projectId}/traceability/overview")
    public ResponseEntity<Map<String, Object>> traceabilityOverview(@PathVariable String projectId) {
        return ResponseEntity.ok(manageCompatibilityService.traceabilityOverview(projectId));
    }

    @GetMapping("/projects/{projectId}/traceability/coverage")
    public ResponseEntity<Map<String, Object>> traceabilityCoverage(@PathVariable String projectId) {
        return ResponseEntity.ok(manageCompatibilityService.traceabilityCoverage(projectId));
    }
}
