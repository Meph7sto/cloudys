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
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.project.dto.CreateBranchRequest;
import com.cloudys.project.dto.CreateMilestoneRequest;
import com.cloudys.project.dto.CreateProjectRequest;
import com.cloudys.project.dto.UpdateProjectRequest;
import com.cloudys.project.service.ProjectManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/manage")
public class ManageController {

    private final ProjectManagementService projectService;

    public ManageController(ProjectManagementService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/projects")
    public ResponseEntity<Map<String, Object>> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.create(request, null));
    }

    @GetMapping("/projects")
    public ResponseEntity<Map<String, Object>> listProjects() {
        return ResponseEntity.ok(projectService.list());
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.get(projectId));
    }

    @PatchMapping("/projects/{projectId}")
    public ResponseEntity<Map<String, Object>> updateProject(@PathVariable String projectId,
                                                              @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.update(projectId, request));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Map<String, Object>> deleteProject(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.archive(projectId));
    }

    @PostMapping("/projects/{projectId}/milestones")
    public ResponseEntity<Map<String, Object>> createMilestone(@PathVariable String projectId,
                                                                @Valid @RequestBody CreateMilestoneRequest request) {
        return ResponseEntity.ok(projectService.createMilestone(projectId, request));
    }

    @GetMapping("/projects/{projectId}/milestones")
    public ResponseEntity<Map<String, Object>> listMilestones(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.listMilestones(projectId));
    }

    @GetMapping("/milestones/{milestoneId}")
    public ResponseEntity<Map<String, Object>> getMilestone(@PathVariable String milestoneId) {
        return ResponseEntity.ok(projectService.getMilestone(milestoneId));
    }

    @PostMapping("/milestones/{milestoneId}/baseline")
    public ResponseEntity<Map<String, Object>> setBaseline(@PathVariable String milestoneId) {
        return ResponseEntity.ok(projectService.markBaseline(milestoneId));
    }

    @PostMapping("/projects/{projectId}/branches")
    public ResponseEntity<Map<String, Object>> createBranch(@PathVariable String projectId,
                                                             @Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.ok(projectService.createBranch(projectId, request));
    }

    @GetMapping("/projects/{projectId}/branches")
    public ResponseEntity<Map<String, Object>> listBranches(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.listBranches(projectId));
    }
}
