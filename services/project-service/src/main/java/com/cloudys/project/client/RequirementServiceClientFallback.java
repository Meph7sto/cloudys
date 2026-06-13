package com.cloudys.project.client;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RequirementServiceClientFallback implements RequirementServiceClient {

    private static Map<String, Object> error(String detail) {
        return Map.of("error", "requirement_service_unavailable", "detail", detail);
    }

    @Override
    public Map<String, Object> listRequirements(String projectId, boolean tree, boolean includeDeleted) {
        return error("需求服务不可用，无法加载需求");
    }

    @Override
    public Map<String, Object> createRequirement(String projectId, Map<String, Object> request) {
        return error("需求服务不可用，无法创建需求");
    }

    @Override
    public Map<String, Object> importRequirements(String projectId, Map<String, Object> request) {
        return error("需求服务不可用，无法导入需求");
    }

    @Override
    public Map<String, Object> getRequirement(String reqId) {
        return error("需求服务不可用，无法加载需求详情");
    }

    @Override
    public Map<String, Object> updateRequirement(String reqId, Map<String, Object> request) {
        return error("需求服务不可用，无法更新需求");
    }

    @Override
    public Map<String, Object> deleteRequirement(String reqId, boolean cascade) {
        return error("需求服务不可用，无法删除需求");
    }

    @Override
    public Map<String, Object> bulkUpdateStatus(Map<String, Object> request) {
        return error("需求服务不可用，无法批量更新状态");
    }

    @Override
    public Map<String, Object> moveRequirement(String reqId, Map<String, Object> request) {
        return error("需求服务不可用，无法移动需求");
    }

    @Override
    public Map<String, Object> listProjectDefects(String projectId) {
        return error("需求服务不可用，无法加载缺陷");
    }

    @Override
    public Map<String, Object> listRequirementDefects(String reqId) {
        return error("需求服务不可用，无法加载需求缺陷");
    }

    @Override
    public Map<String, Object> createDefect(String projectId, Map<String, Object> request) {
        return error("需求服务不可用，无法创建缺陷");
    }

    @Override
    public Map<String, Object> updateDefect(String defectId, Map<String, Object> request) {
        return error("需求服务不可用，无法更新缺陷");
    }

    @Override
    public Map<String, Object> deleteDefect(String defectId) {
        return error("需求服务不可用，无法删除缺陷");
    }
}
