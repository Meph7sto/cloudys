package com.cloudys.requirementanalysis.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.cloudys.requirementanalysis.dto.RequirementImportRequest;

@Component
public class RequirementServiceClientFallback implements RequirementServiceClient {

    @Override
    public Map<String, Object> importRequirements(String projectId, RequirementImportRequest request) {
        return Map.of("error", "requirement_service_unavailable",
                "detail", "需求管理服务不可用，无法导入需求");
    }

    @Override
    public Map<String, Object> listRequirements(String projectId) {
        return Map.of("error", "requirement_service_unavailable",
                "detail", "需求管理服务不可用，无法查询需求");
    }
}
