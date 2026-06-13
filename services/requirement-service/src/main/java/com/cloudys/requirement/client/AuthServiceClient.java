package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service",
        contextId = "requirementAuthPermissionClient",
        path = "/api/v2/permission",
        fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/projects/{projectId}/context")
    Map<String, Object> getPermissionContext(@PathVariable String projectId);
}
