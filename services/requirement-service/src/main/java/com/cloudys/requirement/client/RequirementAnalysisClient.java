package com.cloudys.requirement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cloudys.requirement.dto.AnalysisImportResponse;

@FeignClient(name = "requirement-analysis-service", fallback = RequirementAnalysisClientFallback.class)
public interface RequirementAnalysisClient {

    @GetMapping("/api/v2/analysis/sessions/{sessionId}/requirements-export")
    AnalysisImportResponse exportSessionRequirements(@PathVariable String sessionId);
}
