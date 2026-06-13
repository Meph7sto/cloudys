package com.cloudys.project.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.project.client.RequirementServiceClient;

@Service
public class ManageCompatibilityService {

    private final RequirementServiceClient requirementServiceClient;
    private final AuditLogService auditLogService;
    private final ProjectManagementService projectManagementService;

    public ManageCompatibilityService(RequirementServiceClient requirementServiceClient,
                                      AuditLogService auditLogService,
                                      ProjectManagementService projectManagementService) {
        this.requirementServiceClient = requirementServiceClient;
        this.auditLogService = auditLogService;
        this.projectManagementService = projectManagementService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRequirements(String projectId, boolean tree) {
        return unwrap(requirementServiceClient.listRequirements(projectId, tree, false));
    }

    @Transactional
    public Map<String, Object> createRequirement(String projectId, Map<String, Object> request) {
        Map<String, Object> response = unwrap(requirementServiceClient.createRequirement(projectId, request));
        auditLogService.record(projectId,
                String.valueOf(projectManagementService.get(projectId).get("product_id")),
                "requirement.create",
                "requirement",
                String.valueOf(response.get("req_id")),
                Map.of("description", "创建需求 " + response.getOrDefault("title", response.getOrDefault("req_id", ""))));
        return response;
    }

    @Transactional
    public Map<String, Object> updateRequirement(String reqId, Map<String, Object> request) {
        Map<String, Object> response = unwrap(requirementServiceClient.updateRequirement(reqId, request));
        auditLogService.record(String.valueOf(response.get("project_id")),
                null,
                "requirement.update",
                "requirement",
                reqId,
                Map.of("description", "更新需求 " + response.getOrDefault("title", reqId)));
        return response;
    }

    @Transactional
    public Map<String, Object> bulkUpdateRequirementStatus(Map<String, Object> request) {
        return unwrap(requirementServiceClient.bulkUpdateStatus(request));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listDefects(String projectId) {
        return unwrap(requirementServiceClient.listProjectDefects(projectId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRequirementDefects(String reqId) {
        return unwrap(requirementServiceClient.listRequirementDefects(reqId));
    }

    @Transactional
    public Map<String, Object> createDefect(String projectId, Map<String, Object> request) {
        Map<String, Object> response = unwrap(requirementServiceClient.createDefect(projectId, request));
        auditLogService.record(projectId,
                String.valueOf(projectManagementService.get(projectId).get("product_id")),
                "defect.create",
                "defect",
                String.valueOf(response.get("defect_id")),
                Map.of("description", "创建缺陷 " + response.getOrDefault("title", response.getOrDefault("defect_id", ""))));
        return response;
    }

    @Transactional
    public Map<String, Object> updateDefect(String defectId, Map<String, Object> request) {
        Map<String, Object> response = unwrap(requirementServiceClient.updateDefect(defectId, request));
        auditLogService.record(String.valueOf(response.get("project_id")),
                null,
                "defect.update",
                "defect",
                defectId,
                Map.of("description", "更新缺陷 " + response.getOrDefault("title", defectId)));
        return response;
    }

    @Transactional
    public Map<String, Object> deleteDefect(String defectId) {
        return unwrap(requirementServiceClient.deleteDefect(defectId));
    }

    @Transactional
    public Map<String, Object> moveRequirement(String reqId, Map<String, Object> request) {
        return unwrap(requirementServiceClient.moveRequirement(reqId, request));
    }

    @Transactional
    public Map<String, Object> deleteRequirement(String reqId, boolean cascade) {
        return unwrap(requirementServiceClient.deleteRequirement(reqId, cascade));
    }

    @Transactional
    public Map<String, Object> importRequirements(String projectId, Map<String, Object> request) {
        Map<String, Object> response = unwrap(requirementServiceClient.importRequirements(projectId, request));
        auditLogService.record(projectId,
                String.valueOf(projectManagementService.get(projectId).get("product_id")),
                "requirement.import",
                "project",
                projectId,
                Map.of("description", "导入 Session 需求到项目", "session_id", request.get("session_id")));
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAudits(String projectId, int limit) {
        return auditLogService.listProjectAudits(projectId, limit);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> traceabilityOverview(String projectId) {
        int requirementCount = sizeOfList(listRequirements(projectId, false).get("requirements"));
        int testCaseCount = 0;
        int changeCount = 0;
        int auditCount = sizeOfList(listAudits(projectId, 200).get("logs"));
        return Map.of(
                "summary", Map.of(
                        "requirement_count", requirementCount,
                        "test_case_count", testCaseCount,
                        "change_count", changeCount,
                        "audit_count", auditCount
                )
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> traceabilityCoverage(String projectId) {
        Map<String, Object> requirements = listRequirements(projectId, false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) requirements.getOrDefault("requirements", List.of());
        int total = rows.size();
        int covered = 0;
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) row.getOrDefault("test_case_links", List.of());
            if (links != null && !links.isEmpty()) {
                covered++;
            }
        }
        return Map.of("coverage", Map.of("covered", covered, "total", total));
    }

    private Map<String, Object> unwrap(Map<String, Object> response) {
        if (response.containsKey("error")) {
            throw new ErrorResponse(String.valueOf(response.getOrDefault("detail", "下游服务调用失败")), 503);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private int sizeOfList(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        return 0;
    }
}
