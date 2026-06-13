package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProjectServiceClientFallback implements ProjectServiceClient {

    @Override
    public Map<String, Object> getProject(String projectId) {
        return Map.of("error", "project_service_unavailable", "detail", "项目服务不可用，无法校验项目");
    }
}
