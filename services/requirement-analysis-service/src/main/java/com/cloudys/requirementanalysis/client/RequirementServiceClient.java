package com.cloudys.requirementanalysis.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cloudys.requirementanalysis.dto.RequirementImportRequest;

@FeignClient(name = "requirement-service", fallback = RequirementServiceClientFallback.class)
public interface RequirementServiceClient {

    @PostMapping("/api/v2/requirements/projects/{projectId}/requirements/import")
    Map<String, Object> importRequirements(@PathVariable String projectId, @RequestBody RequirementImportRequest request);

    @GetMapping("/api/v2/requirements/projects/{projectId}/requirements")
    Map<String, Object> listRequirements(@PathVariable String projectId);
}
