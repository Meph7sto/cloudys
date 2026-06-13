package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "project-service", path = "/api/v2/manage", fallback = ProjectServiceClientFallback.class)
public interface ProjectServiceClient {

    @GetMapping("/projects/{projectId}")
    Map<String, Object> getProject(@PathVariable String projectId);
}
