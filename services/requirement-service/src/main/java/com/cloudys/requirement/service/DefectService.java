package com.cloudys.requirement.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirement.dto.CreateDefectRequest;
import com.cloudys.requirement.dto.UpdateDefectRequest;
import com.cloudys.requirement.entity.Defect;
import com.cloudys.requirement.entity.ManageRequirement;
import com.cloudys.requirement.repository.DefectRepository;
import com.cloudys.requirement.repository.ManageRequirementRepository;
import com.cloudys.requirement.util.SecurityUtils;

@Service
public class DefectService {

    private static final Set<String> ALLOWED_SEVERITIES = Set.of("critical", "high", "medium", "low");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("P0", "P1", "P2", "P3");
    private static final Set<String> ALLOWED_STATUSES = Set.of("open", "in_progress", "resolved", "verified", "closed", "rejected");

    private final DefectRepository defectRepository;
    private final ManageRequirementRepository requirementRepository;
    private final RequirementService requirementService;

    public DefectService(DefectRepository defectRepository,
                         ManageRequirementRepository requirementRepository,
                         RequirementService requirementService) {
        this.defectRepository = defectRepository;
        this.requirementRepository = requirementRepository;
        this.requirementService = requirementService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listDefectsByProject(String projectId) {
        requirementService.requireProjectPermission(projectId, RequirementService.PERMISSION_VIEW_REQUIREMENT, false);
        return Map.of("defects", defectRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toMap)
                .toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listDefectsByRequirement(String reqId) {
        ManageRequirement requirement = getRequirement(reqId);
        requirementService.requireProjectPermission(requirement.getProjectId(), RequirementService.PERMISSION_VIEW_REQUIREMENT, false);
        return Map.of("defects", defectRepository.findByRequirementIdOrderByCreatedAtDesc(reqId).stream()
                .map(this::toMap)
                .toList());
    }

    @Transactional
    public Map<String, Object> createDefect(String projectId, CreateDefectRequest request) {
        requirementService.requireProjectPermission(projectId, RequirementService.PERMISSION_EDIT_REQUIREMENT, true);
        ManageRequirement requirement = getRequirement(request.requirementId());
        if (!projectId.equals(requirement.getProjectId())) {
            throw new ErrorResponse("关联需求不属于当前项目", 400);
        }

        String title = trimToValue(request.title(), "缺陷标题不能为空");
        String steps = trimToValue(request.reproduceSteps(), "复现步骤不能为空");
        String severity = normalizeSeverity(request.severity());
        String priority = normalizePriority(request.priority());
        String status = normalizeStatus(request.status());

        String devAssignee = strOrEmpty(request.devAssignee());
        String testerAssignee = strOrEmpty(request.testerAssignee());
        String reporter = strOrEmpty(request.reporter());
        String currentAssignee = resolveCurrentAssignee(status, devAssignee, testerAssignee, strOrEmpty(request.currentAssignee()));

        Defect defect = new Defect();
        defect.setDefectId("def-" + UUID.randomUUID());
        defect.setProjectId(projectId);
        defect.setRequirementId(requirement.getReqId());
        defect.setTitle(title);
        defect.setReproduceSteps(steps);
        defect.setSeverity(severity);
        defect.setPriority(priority);
        defect.setStatus(status);
        defect.setReporter(reporter);
        defect.setDevAssignee(devAssignee);
        defect.setTesterAssignee(testerAssignee);
        defect.setCurrentAssignee(currentAssignee);
        defect.setCreatedBy(SecurityUtils.getCurrentUserId());
        defect.setUpdatedBy(SecurityUtils.getCurrentUserId());

        return toMap(defectRepository.save(defect));
    }

    @Transactional
    public Map<String, Object> updateDefect(String defectId, UpdateDefectRequest request) {
        Defect defect = defectRepository.findById(defectId)
                .orElseThrow(() -> new ErrorResponse("缺陷不存在", 404));
        requirementService.requireProjectPermission(defect.getProjectId(), RequirementService.PERMISSION_EDIT_REQUIREMENT, true);

        if (request.title() != null) {
            defect.setTitle(trimToValue(request.title(), "缺陷标题不能为空"));
        }
        if (request.reproduceSteps() != null) {
            defect.setReproduceSteps(trimToValue(request.reproduceSteps(), "复现步骤不能为空"));
        }
        if (request.requirementId() != null) {
            ManageRequirement requirement = getRequirement(request.requirementId());
            if (!defect.getProjectId().equals(requirement.getProjectId())) {
                throw new ErrorResponse("关联需求不属于当前项目", 400);
            }
            defect.setRequirementId(requirement.getReqId());
        }
        if (request.severity() != null) {
            defect.setSeverity(normalizeSeverity(request.severity()));
        }
        if (request.priority() != null) {
            defect.setPriority(normalizePriority(request.priority()));
        }
        if (request.status() != null) {
            defect.setStatus(normalizeStatus(request.status()));
        }
        if (request.reporter() != null) {
            defect.setReporter(strOrEmpty(request.reporter()));
        }
        if (request.devAssignee() != null) {
            defect.setDevAssignee(strOrEmpty(request.devAssignee()));
        }
        if (request.testerAssignee() != null) {
            defect.setTesterAssignee(strOrEmpty(request.testerAssignee()));
        }

        boolean assigneeChanged = request.status() != null || request.devAssignee() != null
                || request.testerAssignee() != null || request.currentAssignee() != null;
        if (assigneeChanged) {
            String explicit = request.currentAssignee() != null ? strOrEmpty(request.currentAssignee()) : defect.getCurrentAssignee();
            defect.setCurrentAssignee(resolveCurrentAssignee(defect.getStatus(), defect.getDevAssignee(), defect.getTesterAssignee(), explicit));
        }

        defect.setUpdatedBy(SecurityUtils.getCurrentUserId());
        return toMap(defectRepository.save(defect));
    }

    @Transactional
    public Map<String, Object> deleteDefect(String defectId) {
        Defect defect = defectRepository.findById(defectId)
                .orElseThrow(() -> new ErrorResponse("缺陷不存在", 404));
        requirementService.requireProjectPermission(defect.getProjectId(), RequirementService.PERMISSION_EDIT_REQUIREMENT, true);
        defectRepository.delete(defect);
        return Map.of("message", "缺陷已删除");
    }

    static String resolveCurrentAssignee(String status, String devAssignee, String testerAssignee, String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        if (Set.of("open", "in_progress").contains(status)) {
            return devAssignee != null ? devAssignee : "";
        }
        if ("resolved".equals(status)) {
            return testerAssignee != null ? testerAssignee : "";
        }
        return "";
    }

    static String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "medium";
        }
        String normalized = severity.trim().toLowerCase();
        if (!ALLOWED_SEVERITIES.contains(normalized)) {
            throw new ErrorResponse("severity 必须是: " + String.join(", ", ALLOWED_SEVERITIES), 400);
        }
        return normalized;
    }

    static String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "P2";
        }
        String normalized = priority.trim().toUpperCase();
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new ErrorResponse("priority 必须是: " + String.join(", ", ALLOWED_PRIORITIES), 400);
        }
        return normalized;
    }

    static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "open";
        }
        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ErrorResponse("status 必须是: " + String.join(", ", ALLOWED_STATUSES), 400);
        }
        return normalized;
    }

    private ManageRequirement getRequirement(String reqId) {
        return requirementRepository.findByReqIdAndDeletedFalse(reqId)
                .orElseThrow(() -> new ErrorResponse("关联需求不存在", 400));
    }

    private static String strOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String trimToValue(String value, String errorMessage) {
        String trimmed = value != null ? value.trim() : "";
        if (trimmed.isEmpty()) {
            throw new ErrorResponse(errorMessage, 400);
        }
        return trimmed;
    }

    private Map<String, Object> toMap(Defect defect) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("defect_id", defect.getDefectId());
        map.put("project_id", defect.getProjectId());
        map.put("requirement_id", defect.getRequirementId());
        map.put("title", defect.getTitle());
        map.put("reproduce_steps", defect.getReproduceSteps());
        map.put("severity", defect.getSeverity());
        map.put("priority", defect.getPriority());
        map.put("status", defect.getStatus());
        map.put("reporter", defect.getReporter());
        map.put("dev_assignee", defect.getDevAssignee());
        map.put("tester_assignee", defect.getTesterAssignee());
        map.put("current_assignee", defect.getCurrentAssignee());
        map.put("created_by", defect.getCreatedBy());
        map.put("created_at", defect.getCreatedAt());
        map.put("updated_by", defect.getUpdatedBy());
        map.put("updated_at", defect.getUpdatedAt());
        return map;
    }
}
