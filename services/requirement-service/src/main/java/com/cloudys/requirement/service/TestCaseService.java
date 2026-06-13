package com.cloudys.requirement.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirement.dto.CreateTestCaseRequest;
import com.cloudys.requirement.entity.ManageRequirement;
import com.cloudys.requirement.entity.RequirementTestLink;
import com.cloudys.requirement.entity.TestCase;
import com.cloudys.requirement.repository.ManageRequirementRepository;
import com.cloudys.requirement.repository.RequirementTestLinkRepository;
import com.cloudys.requirement.repository.TestCaseRepository;
import com.cloudys.requirement.util.SecurityUtils;

@Service
public class TestCaseService {

    private static final Set<String> ALLOWED_TEST_CASE_STATUSES = Set.of("draft", "active", "deprecated");
    private static final Set<String> ALLOWED_LINK_TYPES = Set.of("verification", "coverage", "regression");

    private final TestCaseRepository testCaseRepository;
    private final ManageRequirementRepository requirementRepository;
    private final RequirementTestLinkRepository requirementTestLinkRepository;
    private final RequirementService requirementService;

    public TestCaseService(TestCaseRepository testCaseRepository,
                           ManageRequirementRepository requirementRepository,
                           RequirementTestLinkRepository requirementTestLinkRepository,
                           RequirementService requirementService) {
        this.testCaseRepository = testCaseRepository;
        this.requirementRepository = requirementRepository;
        this.requirementTestLinkRepository = requirementTestLinkRepository;
        this.requirementService = requirementService;
    }

    @Transactional
    public Map<String, Object> createTestCase(String projectId, CreateTestCaseRequest request) {
        requirementService.requireProjectPermission(projectId, RequirementService.PERMISSION_EDIT_REQUIREMENT, true);
        TestCase tc = new TestCase();
        tc.setTestCaseId("tc-" + UUID.randomUUID());
        tc.setProjectId(projectId);
        tc.setTitle(trimToValue(request.title(), "测试用例标题不能为空"));
        tc.setDescription(request.description() != null ? request.description() : "");
        tc.setStatus(normalizeStatus(request.status()));
        tc.setSource(trimToNull(request.source()));
        tc.setCreatedBy(SecurityUtils.getCurrentUserId());
        return toMap(testCaseRepository.save(tc));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listTestCases(String projectId) {
        requirementService.requireProjectPermission(projectId, RequirementService.PERMISSION_VIEW_REQUIREMENT, false);
        return Map.of("test_cases", testCaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toMap)
                .toList());
    }

    @Transactional
    public Map<String, Object> bindTestCase(String reqId, String testCaseId, String linkType) {
        ManageRequirement req = requirementRepository.findByReqIdAndDeletedFalse(reqId)
                .orElseThrow(() -> new ErrorResponse("需求不存在", 404));
        requirementService.requireProjectPermission(req.getProjectId(), RequirementService.PERMISSION_EDIT_REQUIREMENT, true);
        if (!"low_level".equals(req.getRequirementType())) {
            throw new ErrorResponse("仅允许绑定到底层需求", 400);
        }

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ErrorResponse("测试用例不存在", 404));
        if (!req.getProjectId().equals(testCase.getProjectId())) {
            throw new ErrorResponse("测试用例不属于当前项目", 400);
        }

        String normalizedLinkType = normalizeLinkType(linkType);
        RequirementTestLink link = requirementTestLinkRepository.findByRequirementIdAndTestCaseId(reqId, testCaseId)
                .orElseGet(RequirementTestLink::new);
        link.setRequirementId(reqId);
        link.setTestCaseId(testCaseId);
        link.setLinkType(normalizedLinkType);
        RequirementTestLink saved = requirementTestLinkRepository.save(link);

        return Map.of(
                "success", true,
                "link", Map.of(
                        "link_id", saved.getLinkId(),
                        "requirement_id", saved.getRequirementId(),
                        "test_case_id", saved.getTestCaseId(),
                        "link_type", saved.getLinkType(),
                        "created_at", saved.getCreatedAt()
                )
        );
    }

    private Map<String, Object> toMap(TestCase tc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("test_case_id", tc.getTestCaseId());
        map.put("project_id", tc.getProjectId());
        map.put("title", tc.getTitle());
        map.put("description", tc.getDescription());
        map.put("status", tc.getStatus());
        map.put("source", tc.getSource());
        map.put("created_by", tc.getCreatedBy());
        map.put("created_at", tc.getCreatedAt());
        return map;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "draft";
        }
        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_TEST_CASE_STATUSES.contains(normalized)) {
            throw new ErrorResponse("status 必须是: " + String.join(", ", ALLOWED_TEST_CASE_STATUSES), 400);
        }
        return normalized;
    }

    private static String normalizeLinkType(String linkType) {
        if (linkType == null || linkType.isBlank()) {
            return "verification";
        }
        String normalized = linkType.trim().toLowerCase();
        if (!ALLOWED_LINK_TYPES.contains(normalized)) {
            throw new ErrorResponse("link_type 必须是: " + String.join(", ", ALLOWED_LINK_TYPES), 400);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimToValue(String value, String errorMessage) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ErrorResponse(errorMessage, 400);
        }
        return trimmed;
    }
}
