package com.cloudys.project.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/api/v2/permission", fallback = PermissionServiceClientFallback.class)
public interface PermissionServiceClient {

    @GetMapping("/projects/{projectId}/baselines")
    List<Map<String, Object>> listProjectBaselines(@PathVariable String projectId);
}
