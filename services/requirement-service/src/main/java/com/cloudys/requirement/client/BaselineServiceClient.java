package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service",
        contextId = "requirementAuthBaselineClient",
        path = "/api/v2/permission",
        fallback = BaselineServiceClientFallback.class
)
public interface BaselineServiceClient {

    @GetMapping("/baselines/{baselineId}")
    Map<String, Object> getBaseline(@PathVariable Long baselineId);
}
