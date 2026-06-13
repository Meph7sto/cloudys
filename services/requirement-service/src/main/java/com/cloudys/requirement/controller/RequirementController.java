package com.cloudys.requirement.controller;

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

import com.cloudys.requirement.dto.BindTestCaseRequest;
import com.cloudys.requirement.dto.BulkUpdateStatusRequest;
import com.cloudys.requirement.dto.CreateDefectRequest;
import com.cloudys.requirement.dto.CreateRequirementRequest;
import com.cloudys.requirement.dto.CreateTestCaseRequest;
import com.cloudys.requirement.dto.ImportRequirementsRequest;
import com.cloudys.requirement.dto.MoveRequirementRequest;
import com.cloudys.requirement.dto.SetRequirementBaselineRequest;
import com.cloudys.requirement.dto.UpdateDefectRequest;
import com.cloudys.requirement.dto.UpdateRequirementRequest;
import com.cloudys.requirement.service.DefectService;
import com.cloudys.requirement.service.RequirementService;
import com.cloudys.requirement.service.TestCaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final TestCaseService testCaseService;
    private final DefectService defectService;

    public RequirementController(RequirementService requirementService,
                                  TestCaseService testCaseService,
                                  DefectService defectService) {
        this.requirementService = requirementService;
        this.testCaseService = testCaseService;
        this.defectService = defectService;
    }

    // ── Requirements ──

    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<Map<String, Object>> createRequirement(@PathVariable String projectId,
                                                                  @Valid @RequestBody CreateRequirementRequest request) {
        return ResponseEntity.ok(requirementService.createRequirement(projectId, request));
    }

    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<Map<String, Object>> listRequirements(@PathVariable String projectId,
                                                                 @RequestParam(defaultValue = "false") boolean tree,
                                                                 @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(requirementService.listRequirements(projectId, tree, includeDeleted));
    }

    @PostMapping("/projects/{projectId}/requirements/import")
    public ResponseEntity<Map<String, Object>> importRequirements(@PathVariable String projectId,
                                                                    @Valid @RequestBody ImportRequirementsRequest request) {
        return ResponseEntity.ok(requirementService.importFromSession(projectId, request));
    }

    @GetMapping("/{reqId}")
    public ResponseEntity<Map<String, Object>> getRequirement(@PathVariable String reqId) {
        return ResponseEntity.ok(requirementService.getRequirement(reqId));
    }

    @PatchMapping("/{reqId}")
    public ResponseEntity<Map<String, Object>> updateRequirement(@PathVariable String reqId,
                                                                  @RequestBody UpdateRequirementRequest request) {
        return ResponseEntity.ok(requirementService.updateRequirement(reqId, request));
    }

    @DeleteMapping("/{reqId}")
    public ResponseEntity<Map<String, Object>> deleteRequirement(@PathVariable String reqId,
                                                                  @RequestParam(defaultValue = "false") boolean cascade) {
        return ResponseEntity.ok(requirementService.deleteRequirement(reqId, cascade));
    }

    @PostMapping("/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(@Valid @RequestBody BulkUpdateStatusRequest request) {
        return ResponseEntity.ok(requirementService.bulkUpdateStatus(request.reqIds(), request.status()));
    }

    @PostMapping("/{reqId}/move")
    public ResponseEntity<Map<String, Object>> moveRequirement(@PathVariable String reqId,
                                                                 @RequestBody MoveRequirementRequest request) {
        return ResponseEntity.ok(requirementService.moveRequirement(reqId, request));
    }

    @PostMapping("/{reqId}/baseline")
    public ResponseEntity<Map<String, Object>> setRequirementBaseline(@PathVariable String reqId,
                                                                       @Valid @RequestBody SetRequirementBaselineRequest request) {
        return ResponseEntity.ok(requirementService.setBaseline(reqId, request.baselineId()));
    }

    // ── Test Cases ──

    @PostMapping("/projects/{projectId}/test-cases")
    public ResponseEntity<Map<String, Object>> createTestCase(@PathVariable String projectId,
                                                               @Valid @RequestBody CreateTestCaseRequest request) {
        return ResponseEntity.ok(testCaseService.createTestCase(projectId, request));
    }

    @GetMapping("/projects/{projectId}/test-cases")
    public ResponseEntity<Map<String, Object>> listTestCases(@PathVariable String projectId) {
        return ResponseEntity.ok(testCaseService.listTestCases(projectId));
    }

    @PostMapping("/{reqId}/bind-testcase")
    public ResponseEntity<Map<String, Object>> bindTestCase(@PathVariable String reqId,
                                                             @Valid @RequestBody BindTestCaseRequest request) {
        return ResponseEntity.ok(testCaseService.bindTestCase(reqId, request.testCaseId(), request.linkType()));
    }

    // ── Defects ──

    @GetMapping("/projects/{projectId}/defects")
    public ResponseEntity<Map<String, Object>> listDefectsByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(defectService.listDefectsByProject(projectId));
    }

    @GetMapping("/{reqId}/defects")
    public ResponseEntity<Map<String, Object>> listDefectsByRequirement(@PathVariable String reqId) {
        return ResponseEntity.ok(defectService.listDefectsByRequirement(reqId));
    }

    @PostMapping("/projects/{projectId}/defects")
    public ResponseEntity<Map<String, Object>> createDefect(@PathVariable String projectId,
                                                             @Valid @RequestBody CreateDefectRequest request) {
        return ResponseEntity.ok(defectService.createDefect(projectId, request));
    }

    @PatchMapping("/defects/{defectId}")
    public ResponseEntity<Map<String, Object>> updateDefect(@PathVariable String defectId,
                                                             @RequestBody UpdateDefectRequest request) {
        return ResponseEntity.ok(defectService.updateDefect(defectId, request));
    }

    @DeleteMapping("/defects/{defectId}")
    public ResponseEntity<Map<String, Object>> deleteDefect(@PathVariable String defectId) {
        return ResponseEntity.ok(defectService.deleteDefect(defectId));
    }
}
