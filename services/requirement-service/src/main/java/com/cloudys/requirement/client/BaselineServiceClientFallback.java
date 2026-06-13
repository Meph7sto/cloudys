package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BaselineServiceClientFallback implements BaselineServiceClient {

    @Override
    public Map<String, Object> getBaseline(Long baselineId) {
        return Map.of("error", "baseline_service_unavailable", "detail", "基线服务不可用，无法校验基线");
    }
}
