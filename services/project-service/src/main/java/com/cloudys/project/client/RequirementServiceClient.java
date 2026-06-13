package com.cloudys.project.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "requirement-service", path = "/api/v2/requirements", fallback = RequirementServiceClientFallback.class)
public interface RequirementServiceClient {

    @GetMapping("/projects/{projectId}/requirements")
    Map<String, Object> listRequirements(@PathVariable String projectId,
                                         @RequestParam(name = "tree", defaultValue = "false") boolean tree,
                                         @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted);

    @PostMapping("/projects/{projectId}/requirements")
    Map<String, Object> createRequirement(@PathVariable String projectId, @RequestBody Map<String, Object> request);

    @PostMapping("/projects/{projectId}/requirements/import")
    Map<String, Object> importRequirements(@PathVariable String projectId, @RequestBody Map<String, Object> request);

    @GetMapping("/{reqId}")
    Map<String, Object> getRequirement(@PathVariable String reqId);

    @PatchMapping("/{reqId}")
    Map<String, Object> updateRequirement(@PathVariable String reqId, @RequestBody Map<String, Object> request);

    @DeleteMapping("/{reqId}")
    Map<String, Object> deleteRequirement(@PathVariable String reqId,
                                          @RequestParam(name = "cascade", defaultValue = "false") boolean cascade);

    @PostMapping("/bulk-status")
    Map<String, Object> bulkUpdateStatus(@RequestBody Map<String, Object> request);

    @PostMapping("/{reqId}/move")
    Map<String, Object> moveRequirement(@PathVariable String reqId, @RequestBody Map<String, Object> request);

    @GetMapping("/projects/{projectId}/defects")
    Map<String, Object> listProjectDefects(@PathVariable String projectId);

    @GetMapping("/{reqId}/defects")
    Map<String, Object> listRequirementDefects(@PathVariable String reqId);

    @PostMapping("/projects/{projectId}/defects")
    Map<String, Object> createDefect(@PathVariable String projectId, @RequestBody Map<String, Object> request);

    @PatchMapping("/defects/{defectId}")
    Map<String, Object> updateDefect(@PathVariable String defectId, @RequestBody Map<String, Object> request);

    @DeleteMapping("/defects/{defectId}")
    Map<String, Object> deleteDefect(@PathVariable String defectId);
}
