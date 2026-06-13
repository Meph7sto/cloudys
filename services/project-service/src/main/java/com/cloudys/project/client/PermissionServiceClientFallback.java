package com.cloudys.project.client;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PermissionServiceClientFallback implements PermissionServiceClient {

    @Override
    public List<Map<String, Object>> listProjectBaselines(String projectId) {
        return List.of();
    }
}
