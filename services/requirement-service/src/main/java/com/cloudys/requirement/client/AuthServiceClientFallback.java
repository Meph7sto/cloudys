package com.cloudys.requirement.client;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public Map<String, Object> getPermissionContext(String projectId) {
        return Map.of("error", "auth_service_unavailable", "detail", "认证服务不可用，无法校验权限");
    }
}
