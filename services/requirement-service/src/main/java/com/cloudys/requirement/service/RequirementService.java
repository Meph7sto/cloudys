package com.cloudys.requirement.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirement.client.AuthServiceClient;
import com.cloudys.requirement.client.BaselineServiceClient;
import com.cloudys.requirement.client.ProjectServiceClient;
import com.cloudys.requirement.client.RequirementAnalysisClient;
import com.cloudys.requirement.dto.AnalysisImportResponse;
import com.cloudys.requirement.dto.CreateRequirementRequest;
import com.cloudys.requirement.dto.ImportRequirementsRequest;
import com.cloudys.requirement.dto.MoveRequirementRequest;
import com.cloudys.requirement.dto.UpdateRequirementRequest;
import com.cloudys.requirement.entity.ManageRequirement;
import com.cloudys.requirement.entity.RequirementTestLink;
import com.cloudys.requirement.repository.DefectRepository;
import com.cloudys.requirement.repository.ManageRequirementRepository;
import com.cloudys.requirement.repository.RequirementTestLinkRepository;
import com.cloudys.requirement.util.SecurityUtils;

@Service
public class RequirementService {

    static final String PERMISSION_VIEW_REQUIREMENT = "view_requirement";
    static final String PERMISSION_EDIT_REQUIREMENT = "edit_requirement";
    static final String PERMISSION_DELETE_REQUIREMENT = "delete_requirement";

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "draft", "under_review", "confirmed", "in_progress", "completed", "archived"
    );
    private static final Set<String> ALLOWED_TYPES = Set.of("top_level", "low_level", "task");
    private static final Set<String> TOP_LEVEL_SOURCE_LEVELS = Set.of("L1", "L2", "L3");
    private static final Set<String> UNRESOLVED_DEFECT_STATUSES = Set.of("open", "in_progress", "resolved");
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            "draft", Set.of("draft", "under_review", "confirmed", "in_progress", "archived"),
            "under_review", Set.of("under_review", "draft", "confirmed", "archived"),
            "confirmed", Set.of("confirmed", "in_progress", "under_review", "archived"),
            "in_progress", Set.of("in_progress", "under_review", "completed", "archived"),
            "completed", Set.of("completed", "archived"),
            "archived", Set.of("archived")
    );

    private final ManageRequirementRepository requirementRepository;
    private final DefectRepository defectRepository;
    private final RequirementTestLinkRepository requirementTestLinkRepository;
    private final AuthServiceClient authServiceClient;
    private final ProjectServiceClient projectServiceClient;
    private final BaselineServiceClient baselineServiceClient;
    private final RequirementAnalysisClient requirementAnalysisClient;

    public RequirementService(ManageRequirementRepository requirementRepository,
                              DefectRepository defectRepository,
                              RequirementTestLinkRepository requirementTestLinkRepository,
                              AuthServiceClient authServiceClient,
                              ProjectServiceClient projectServiceClient,
                              BaselineServiceClient baselineServiceClient,
                              RequirementAnalysisClient requirementAnalysisClient) {
        this.requirementRepository = requirementRepository;
        this.defectRepository = defectRepository;
        this.requirementTestLinkRepository = requirementTestLinkRepository;
        this.authServiceClient = authServiceClient;
        this.projectServiceClient = projectServiceClient;
        this.baselineServiceClient = baselineServiceClient;
        this.requirementAnalysisClient = requirementAnalysisClient;
    }

    @Transactional
    public Map<String, Object> createRequirement(String projectId, CreateRequirementRequest request) {
        requireProjectPermission(projectId, PERMISSION_EDIT_REQUIREMENT, true);
        String type = normalizeType(request.requirementType(), request.sourceLevel());
        String status = normalizeStatus(request.status(), true);
        String priority = normalizePriority(request.priority(), true);

        ManageRequirement parent = null;
        if (request.parentId() != null && !request.parentId().isBlank()) {
            parent = getRequired(request.parentId());
            validateParent(projectId, parent, type);
        }

        ManageRequirement req = new ManageRequirement();
        req.setReqId("req-" + UUID.randomUUID());
        req.setProjectId(projectId);
        req.setRequirementType(type);
        req.setStatus(status);
        req.setTitle(trimToValue(request.title(), "标题不能为空"));
        req.setDescription(blankToEmpty(request.description()));
        req.setPriority(priority);
        req.setAssignee(trimToNull(request.assignee()));
        req.setTags(JsonSupport.toJson(request.tags()));
        req.setDueDate(trimToNull(request.dueDate()));
        req.setParentId(parent != null ? parent.getReqId() : null);
        req.setOrderIndex(request.orderIndex() != null ? request.orderIndex() : 0);
        req.setSourceReqId(trimToNull(request.sourceReqId()));
        req.setSourceLevel(normalizeSourceLevel(request.sourceLevel(), type));
        req.setCustomFields(JsonSupport.toJson(request.customFields()));
        req.setCreatedBy(SecurityUtils.getCurrentUserId());
        req.setUpdatedBy(SecurityUtils.getCurrentUserId());

        return toMap(requirementRepository.save(req));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRequirements(String projectId, boolean tree, boolean includeDeleted) {
        requireProjectPermission(projectId, PERMISSION_VIEW_REQUIREMENT, false);
        List<ManageRequirement> requirements = includeDeleted
                ? requirementRepository.findByProjectIdOrderByOrderIndexAscCreatedAtDesc(projectId)
                : requirementRepository.findByProjectIdAndDeletedFalseOrderByOrderIndexAscCreatedAtDesc(projectId);

        if (tree) {
            return Map.of("requirements", buildTree(requirements));
        }
        return Map.of("requirements", requirements.stream().map(this::toMap).toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRequirement(String reqId) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_VIEW_REQUIREMENT, false);
        return toMap(req);
    }

    @Transactional
    public Map<String, Object> updateRequirement(String reqId, UpdateRequirementRequest request) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_EDIT_REQUIREMENT, true);

        if (request.title() != null) {
            req.setTitle(trimToValue(request.title(), "标题不能为空"));
        }
        if (request.description() != null) {
            req.setDescription(request.description());
        }
        if (request.priority() != null) {
            req.setPriority(normalizePriority(request.priority(), true));
        }
        if (request.assignee() != null) {
            req.setAssignee(trimToNull(request.assignee()));
        }
        if (request.tags() != null) {
            req.setTags(JsonSupport.toJson(request.tags()));
        }
        if (request.dueDate() != null) {
            req.setDueDate(trimToNull(request.dueDate()));
        }
        if (request.customFields() != null) {
            req.setCustomFields(JsonSupport.toJson(request.customFields()));
        }
        if (request.orderIndex() != null) {
            req.setOrderIndex(request.orderIndex());
        }
        if (request.status() != null) {
            String targetStatus = normalizeStatus(request.status(), false);
            validateStatusTransition(req.getStatus(), targetStatus, req.getRequirementType());
            ensureDeliverable(reqId, targetStatus);
            req.setStatus(targetStatus);
        }

        req.setUpdatedBy(SecurityUtils.getCurrentUserId());
        return toMap(requirementRepository.save(req));
    }

    @Transactional
    public Map<String, Object> bulkUpdateStatus(List<String> reqIds, String status) {
        if (reqIds == null || reqIds.isEmpty()) {
            throw new ErrorResponse("缺少需求ID列表", 400);
        }
        String targetStatus = normalizeStatus(status, false);
        List<ManageRequirement> rows = requirementRepository.findAllById(reqIds);
        if (rows.size() != reqIds.size()) {
            Set<String> existing = rows.stream().map(ManageRequirement::getReqId).collect(Collectors.toSet());
            String missing = reqIds.stream().filter(id -> !existing.contains(id)).findFirst().orElse("unknown");
            throw new ErrorResponse("需求不存在: " + missing, 404);
        }

        ensureSameProjectPermission(rows, PERMISSION_EDIT_REQUIREMENT, true);

        List<ManageRequirement> toUpdate = new ArrayList<>();
        int skipped = 0;
        for (ManageRequirement row : rows) {
            if (Boolean.TRUE.equals(row.getDeleted())) {
                skipped++;
                continue;
            }
            if (!"low_level".equals(row.getRequirementType())) {
                skipped++;
                continue;
            }
            validateStatusTransition(row.getStatus(), targetStatus, row.getRequirementType());
            ensureDeliverable(row.getReqId(), targetStatus);
            row.setStatus(targetStatus);
            row.setUpdatedBy(SecurityUtils.getCurrentUserId());
            toUpdate.add(row);
        }

        requirementRepository.saveAll(toUpdate);
        return Map.of("updated", toUpdate.size(), "skipped", skipped);
    }

    @Transactional
    public Map<String, Object> moveRequirement(String reqId, MoveRequirementRequest request) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_EDIT_REQUIREMENT, true);
        String newParentId = trimToNull(request.parentId());

        if (newParentId != null) {
            ManageRequirement parent = getRequired(newParentId);
            validateParent(req.getProjectId(), parent, req.getRequirementType());
            List<ManageRequirement> allReqs = requirementRepository.findByProjectIdAndDeletedFalse(req.getProjectId());
            List<Map<String, Object>> tree = buildTree(allReqs);
            if (isDescendant(tree, reqId, newParentId)) {
                throw new ErrorResponse("不能移动到自己的后代节点", 400);
            }
        }

        req.setParentId(newParentId);
        req.setOrderIndex(request.orderIndex() != null ? request.orderIndex() : 0);
        req.setUpdatedBy(SecurityUtils.getCurrentUserId());
        requirementRepository.save(req);
        return Map.of("message", "需求已移动");
    }

    @Transactional
    public Map<String, Object> deleteRequirement(String reqId, boolean cascade) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_DELETE_REQUIREMENT, true);

        List<String> targets = new ArrayList<>();
        targets.add(reqId);
        if (cascade) {
            List<ManageRequirement> allReqs = requirementRepository.findByProjectId(req.getProjectId());
            List<Map<String, Object>> tree = buildTree(allReqs);
            targets = collectDescendants(tree, reqId);
        }

        List<ManageRequirement> toDelete = requirementRepository.findAllById(targets);
        toDelete.forEach(r -> {
            r.setDeleted(true);
            r.setUpdatedBy(SecurityUtils.getCurrentUserId());
        });
        requirementRepository.saveAll(toDelete);
        return Map.of("deleted", toDelete.size());
    }

    @Transactional
    public Map<String, Object> importFromSession(String projectId, ImportRequirementsRequest request) {
        requireProjectPermission(projectId, PERMISSION_EDIT_REQUIREMENT, true);
        String mappingMode = request.mappingMode() != null ? request.mappingMode().trim().toLowerCase() : "tree";
        if (!Set.of("tree", "flat").contains(mappingMode)) {
            throw new ErrorResponse("mapping_mode 必须是 tree 或 flat", 400);
        }

        AnalysisImportResponse export = requirementAnalysisClient.exportSessionRequirements(request.sessionId());
        List<ManageRequirement> existingRows = requirementRepository.findByProjectIdAndDeletedFalse(projectId);
        Map<String, String> existingBySource = existingRows.stream()
                .filter(row -> row.getSourceReqId() != null && !row.getSourceReqId().isBlank())
                .collect(Collectors.toMap(ManageRequirement::getSourceReqId, ManageRequirement::getReqId, (left, right) -> left));

        int insertedL123 = 0;
        int insertedL4 = 0;
        int unlinkedL4 = 0;
        Map<String, Integer> levelCounts = new LinkedHashMap<>();

        for (AnalysisImportResponse.AnalysisRequirementRecord row : export.safeRequirementsL123()) {
            String level = trimToNull(row.level());
            if (level == null) {
                continue;
            }
            levelCounts.merge(level, 1, Integer::sum);
        }

        for (AnalysisImportResponse.AnalysisRequirementRecord row : export.safeRequirementsL123()) {
            String sourceReqId = trimToNull(row.reqId());
            if (sourceReqId == null || existingBySource.containsKey(sourceReqId)) {
                continue;
            }
            String type = normalizeType(null, row.level());
            ManageRequirement req = buildImportedRequirement(
                    projectId,
                    type,
                    trimToValue(row.text(), "导入需求文本不能为空"),
                    sourceReqId,
                    row.level(),
                    null,
                    Map.of("source", "requirements_l123")
            );
            ManageRequirement saved = requirementRepository.save(req);
            existingBySource.put(sourceReqId, saved.getReqId());
            insertedL123++;
        }

        Map<String, List<String>> linkMap = new LinkedHashMap<>();
        for (AnalysisImportResponse.AnalysisLowLevelLinkRecord link : export.safeLowLevelRequirementLinks()) {
            String reqId = trimToNull(link.reqId());
            String topReqId = trimToNull(link.topReqId());
            if (reqId != null && topReqId != null) {
                linkMap.computeIfAbsent(reqId, key -> new ArrayList<>()).add(topReqId);
            }
        }

        levelCounts.put("L4", export.safeLowLevelRequirements().size());
        for (AnalysisImportResponse.AnalysisLowLevelRequirementRecord row : export.safeLowLevelRequirements()) {
            String sourceReqId = trimToNull(row.reqId());
            if (sourceReqId == null) {
                continue;
            }
            String parentId = resolveImportedParent(mappingMode, row.sourceTopId(), sourceReqId, existingBySource, linkMap);
            if (!"flat".equals(mappingMode) && parentId == null) {
                unlinkedL4++;
            }

            if (existingBySource.containsKey(sourceReqId)) {
                if (parentId != null) {
                    ManageRequirement existing = getRequired(existingBySource.get(sourceReqId));
                    if (existing.getParentId() == null) {
                        existing.setParentId(parentId);
                        existing.setUpdatedBy(SecurityUtils.getCurrentUserId());
                        requirementRepository.save(existing);
                    }
                }
                continue;
            }

            Map<String, Object> customFields = new LinkedHashMap<>();
            customFields.put("source", "low_level_requirements");
            if (row.sourceTopId() != null) {
                customFields.put("source_top_id", row.sourceTopId());
            }
            if (row.sourceTopText() != null) {
                customFields.put("source_top_text", row.sourceTopText());
            }
            if (row.component() != null) {
                customFields.put("component", row.component());
            }
            if (row.acceptanceCriteria() != null) {
                customFields.put("acceptance_criteria", row.acceptanceCriteria());
            }
            if (row.testMethod() != null) {
                customFields.put("test_method", row.testMethod());
            }
            if (row.meta() != null && !row.meta().isEmpty()) {
                customFields.put("meta", row.meta());
            }

            ManageRequirement req = buildImportedRequirement(
                    projectId,
                    "low_level",
                    trimToValue(row.text(), "导入 L4 文本不能为空"),
                    sourceReqId,
                    "L4",
                    parentId,
                    customFields
            );
            ManageRequirement saved = requirementRepository.save(req);
            existingBySource.put(sourceReqId, saved.getReqId());
            insertedL4++;
        }

        int topLevelTotal = TOP_LEVEL_SOURCE_LEVELS.stream().mapToInt(level -> levelCounts.getOrDefault(level, 0)).sum();
        int total = levelCounts.values().stream().mapToInt(Integer::intValue).sum();

        return Map.of(
                "inserted", insertedL123 + insertedL4,
                "inserted_l123", insertedL123,
                "inserted_l4", insertedL4,
                "total", total,
                "mapping_mode", mappingMode,
                "stats", Map.of(
                        "by_level", levelCounts,
                        "top_level_total", topLevelTotal,
                        "low_level_total", levelCounts.getOrDefault("L4", 0),
                        "unlinked_l4", unlinkedL4
                )
        );
    }

    @Transactional
    public Map<String, Object> setBaseline(String reqId, Long baselineId) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_EDIT_REQUIREMENT, true);
        if (baselineId != null) {
            validateBaseline(req.getProjectId(), baselineId);
        }
        req.setBaselineId(baselineId);
        req.setUpdatedBy(SecurityUtils.getCurrentUserId());
        return toMap(requirementRepository.save(req));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRequirementTestLinks(String reqId) {
        ManageRequirement req = getRequired(reqId);
        requireProjectPermission(req.getProjectId(), PERMISSION_VIEW_REQUIREMENT, false);
        return requirementTestLinkRepository.findByRequirementIdOrderByCreatedAtDesc(reqId).stream()
                .map(this::toLinkMap)
                .toList();
    }

    public List<Map<String, Object>> buildTree(List<ManageRequirement> requirements) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (ManageRequirement req : requirements) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("requirement", toMap(req));
            node.put("children", new ArrayList<>());
            nodeMap.put(req.getReqId(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (ManageRequirement req : requirements) {
            Map<String, Object> node = nodeMap.get(req.getReqId());
            String parentId = req.getParentId();
            if (parentId != null && nodeMap.containsKey(parentId)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }

        for (Map<String, Object> node : nodeMap.values()) {
            sortTreeChildren(node);
        }
        roots.sort(this::compareTreeNodes);
        return roots;
    }

    private void sortTreeChildren(Map<String, Object> node) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        children.sort(this::compareTreeNodes);
        for (Map<String, Object> child : children) {
            sortTreeChildren(child);
        }
    }

    private int compareTreeNodes(Map<String, Object> left, Map<String, Object> right) {
        @SuppressWarnings("unchecked")
        Map<String, Object> leftReq = (Map<String, Object>) left.get("requirement");
        @SuppressWarnings("unchecked")
        Map<String, Object> rightReq = (Map<String, Object>) right.get("requirement");
        int leftOrder = leftReq.get("order_index") instanceof Number n ? n.intValue() : 0;
        int rightOrder = rightReq.get("order_index") instanceof Number n ? n.intValue() : 0;
        return Integer.compare(leftOrder, rightOrder);
    }

    private ManageRequirement buildImportedRequirement(String projectId,
                                                       String type,
                                                       String title,
                                                       String sourceReqId,
                                                       String sourceLevel,
                                                       String parentId,
                                                       Map<String, Object> customFields) {
        ManageRequirement req = new ManageRequirement();
        req.setReqId("req-" + UUID.randomUUID());
        req.setProjectId(projectId);
        req.setRequirementType(type);
        req.setStatus("draft");
        req.setTitle(title);
        req.setDescription("");
        req.setTags(JsonSupport.toJson(List.of()));
        req.setParentId(parentId);
        req.setOrderIndex(0);
        req.setSourceReqId(sourceReqId);
        req.setSourceLevel(sourceLevel);
        req.setCustomFields(JsonSupport.toJson(customFields));
        req.setCreatedBy(SecurityUtils.getCurrentUserId());
        req.setUpdatedBy(SecurityUtils.getCurrentUserId());
        return req;
    }

    private String resolveImportedParent(String mappingMode,
                                         String sourceTopId,
                                         String lowLevelReqId,
                                         Map<String, String> existingBySource,
                                         Map<String, List<String>> linkMap) {
        if ("flat".equals(mappingMode)) {
            return null;
        }
        LinkedHashSet<String> candidateIds = new LinkedHashSet<>();
        if (linkMap.containsKey(lowLevelReqId)) {
            candidateIds.addAll(linkMap.get(lowLevelReqId));
        }
        if (sourceTopId != null) {
            candidateIds.add(sourceTopId);
        }
        for (String topId : candidateIds) {
            String parentId = existingBySource.get(topId);
            if (parentId != null) {
                return parentId;
            }
        }
        return null;
    }

    private void validateBaseline(String projectId, Long baselineId) {
        Map<String, Object> baseline = baselineServiceClient.getBaseline(baselineId);
        if (baseline.containsKey("error")) {
            throw new ErrorResponse(String.valueOf(baseline.getOrDefault("detail", "基线服务不可用，无法校验基线")), 503);
        }
        if (!projectId.equals(String.valueOf(baseline.get("project_id")))) {
            throw new ErrorResponse("基线不属于当前项目", 400);
        }
    }

    private void validateParent(String projectId, ManageRequirement parent, String childType) {
        if (!projectId.equals(parent.getProjectId())) {
            throw new ErrorResponse("父节点不属于当前项目", 400);
        }
        if (!canHaveChild(parent.getRequirementType(), childType)) {
            throw new ErrorResponse("不符合层级规则: " + parent.getRequirementType() + " 不能包含 " + childType, 400);
        }
    }

    private void ensureSameProjectPermission(Collection<ManageRequirement> requirements, String permission, boolean requireEdit) {
        String projectId = requirements.stream().map(ManageRequirement::getProjectId).distinct().reduce((left, right) -> {
            throw new ErrorResponse("批量操作仅允许同一项目内的需求", 400);
        }).orElseThrow(() -> new ErrorResponse("缺少需求数据", 400));
        requireProjectPermission(projectId, permission, requireEdit);
    }

    void requireProjectPermission(String projectId, String permission, boolean requireEdit) {
        validateProject(projectId);
        Map<String, Object> context = authServiceClient.getPermissionContext(projectId);
        if (context.containsKey("error")) {
            throw new ErrorResponse(String.valueOf(context.getOrDefault("detail", "认证服务不可用，无法校验权限")), 503);
        }

        if (!Boolean.TRUE.equals(context.get("can_access"))) {
            throw new ErrorResponse("无权访问当前项目", 403);
        }
        if (requireEdit && !Boolean.TRUE.equals(context.get("can_edit"))) {
            throw new ErrorResponse("无权修改当前项目需求", 403);
        }
        if (!hasPermission(context.get("permissions"), permission)) {
            throw new ErrorResponse("缺少权限: " + permission, 403);
        }
    }

    private boolean hasPermission(Object permissionsObject, String permission) {
        if (!(permissionsObject instanceof Collection<?> permissions)) {
            return false;
        }
        for (Object value : permissions) {
            if (permission.equals(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    private void validateProject(String projectId) {
        Map<String, Object> project = projectServiceClient.getProject(projectId);
        if (project.containsKey("error")) {
            throw new ErrorResponse(String.valueOf(project.getOrDefault("detail", "项目服务不可用，无法校验项目")), 503);
        }
        if (project.get("project_id") == null) {
            throw new ErrorResponse("项目不存在", 404);
        }
    }

    private boolean isDescendant(List<Map<String, Object>> tree, String ancestorId, String maybeDescendantId) {
        return findAndSearch(tree, ancestorId, maybeDescendantId);
    }

    private boolean findAndSearch(List<Map<String, Object>> nodes, String targetId, String searchId) {
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            Map<String, Object> req = (Map<String, Object>) node.get("requirement");
            if (targetId.equals(req.get("req_id"))) {
                return searchInSubtree(node, searchId);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (findAndSearch(children, targetId, searchId)) {
                return true;
            }
        }
        return false;
    }

    private boolean searchInSubtree(Map<String, Object> node, String searchId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        for (Map<String, Object> child : children) {
            @SuppressWarnings("unchecked")
            Map<String, Object> req = (Map<String, Object>) child.get("requirement");
            if (searchId.equals(req.get("req_id"))) {
                return true;
            }
            if (searchInSubtree(child, searchId)) {
                return true;
            }
        }
        return false;
    }

    private List<String> collectDescendants(List<Map<String, Object>> tree, String targetId) {
        List<String> result = new ArrayList<>();
        collectDescendantsDfs(tree, targetId, false, result);
        return result;
    }

    private void collectDescendantsDfs(List<Map<String, Object>> nodes, String targetId,
                                       boolean collecting, List<String> result) {
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            Map<String, Object> req = (Map<String, Object>) node.get("requirement");
            String reqId = (String) req.get("req_id");
            boolean isTarget = reqId.equals(targetId);
            if (collecting || isTarget) {
                result.add(reqId);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            collectDescendantsDfs(children, targetId, collecting || isTarget, result);
        }
    }

    private void validateStatusTransition(String fromStatus, String toStatus, String type) {
        Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(fromStatus, Set.of(fromStatus));
        if (!allowed.contains(toStatus)) {
            throw new ErrorResponse(
                    "状态流转不允许: " + fromStatus + " -> " + toStatus + "（类型: " + type + "）。允许流转: " + String.join(", ", allowed),
                    400
            );
        }
    }

    private void ensureDeliverable(String reqId, String targetStatus) {
        if (!Set.of("confirmed", "completed").contains(targetStatus)) {
            return;
        }
        long unresolved = defectRepository.countByRequirementIdAndStatusIn(reqId, new ArrayList<>(UNRESOLVED_DEFECT_STATUSES));
        if (unresolved > 0) {
            throw new ErrorResponse("该需求仍有关联未解决缺陷，不能标记为已确认/已完成", 400);
        }
    }

    public static String normalizeStatus(String status, boolean allowNull) {
        if (status == null || status.isBlank()) {
            if (allowNull) {
                return "draft";
            }
            throw new ErrorResponse("status 必须是: " + String.join(", ", ALLOWED_STATUSES), 400);
        }
        String normalized = "backlog".equalsIgnoreCase(status.trim()) ? "draft" : status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ErrorResponse("status 必须是: " + String.join(", ", ALLOWED_STATUSES), 400);
        }
        return normalized;
    }

    public static String normalizePriority(String priority, boolean allowNull) {
        if (priority == null || priority.isBlank()) {
            return allowNull ? null : "medium";
        }
        String normalized = priority.trim().toLowerCase();
        if (!Set.of("low", "medium", "high").contains(normalized)) {
            throw new ErrorResponse("priority 必须是 low、medium、high", 400);
        }
        return normalized;
    }

    public static boolean canHaveChild(String parentType, String childType) {
        return switch (parentType) {
            case "top_level" -> Set.of("low_level", "task").contains(childType);
            case "low_level" -> "task".equals(childType);
            default -> false;
        };
    }

    private String normalizeType(String requestedType, String sourceLevel) {
        String normalizedLevel = trimToNull(sourceLevel);
        if (normalizedLevel != null) {
            return switch (normalizedLevel) {
                case "L1", "L2", "L3" -> "top_level";
                case "L4" -> "low_level";
                case "L5" -> "task";
                default -> throw new ErrorResponse("source_level 必须是 L1-L5", 400);
            };
        }
        if (requestedType == null || requestedType.isBlank()) {
            throw new ErrorResponse("requirement_type 必须是 top_level、low_level 或 task", 400);
        }
        String normalizedType = requestedType.trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new ErrorResponse("requirement_type 必须是 top_level、low_level 或 task", 400);
        }
        return normalizedType;
    }

    private String normalizeSourceLevel(String sourceLevel, String requirementType) {
        String normalized = trimToNull(sourceLevel);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "L1", "L2", "L3" -> {
                if (!"top_level".equals(requirementType)) {
                    throw new ErrorResponse("L1-L3 只能映射为 top_level 需求", 400);
                }
                yield normalized;
            }
            case "L4" -> {
                if (!"low_level".equals(requirementType)) {
                    throw new ErrorResponse("L4 只能映射为 low_level 需求", 400);
                }
                yield normalized;
            }
            case "L5" -> {
                if (!"task".equals(requirementType)) {
                    throw new ErrorResponse("L5 只能映射为 task 需求", 400);
                }
                yield normalized;
            }
            default -> throw new ErrorResponse("source_level 必须是 L1-L5", 400);
        };
    }

    private ManageRequirement getRequired(String reqId) {
        return requirementRepository.findByReqIdAndDeletedFalse(reqId)
                .orElseThrow(() -> new ErrorResponse("需求不存在", 404));
    }

    public Map<String, Object> toMap(ManageRequirement req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", req.getReqId());
        map.put("project_id", req.getProjectId());
        map.put("requirement_type", req.getRequirementType());
        map.put("status", req.getStatus());
        map.put("title", req.getTitle());
        map.put("description", req.getDescription());
        map.put("priority", req.getPriority());
        map.put("assignee", req.getAssignee());
        map.put("tags", JsonSupport.toStringList(req.getTags()));
        map.put("due_date", req.getDueDate());
        map.put("parent_id", req.getParentId());
        map.put("order_index", req.getOrderIndex());
        map.put("source_req_id", req.getSourceReqId());
        map.put("source_level", req.getSourceLevel());
        map.put("custom_fields", JsonSupport.toMap(req.getCustomFields()));
        map.put("baseline_id", req.getBaselineId());
        map.put("created_by", req.getCreatedBy());
        map.put("created_at", req.getCreatedAt());
        map.put("updated_by", req.getUpdatedBy());
        map.put("updated_at", req.getUpdatedAt());
        map.put("deleted", req.getDeleted());
        map.put("is_planned", req.getIsPlanned());
        map.put("test_case_links", listTestLinksInline(req.getReqId()));
        return map;
    }

    private List<Map<String, Object>> listTestLinksInline(String reqId) {
        return requirementTestLinkRepository.findByRequirementIdOrderByCreatedAtDesc(reqId).stream()
                .map(this::toLinkMap)
                .toList();
    }

    private Map<String, Object> toLinkMap(RequirementTestLink link) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("link_id", link.getLinkId());
        map.put("requirement_id", link.getRequirementId());
        map.put("test_case_id", link.getTestCaseId());
        map.put("link_type", link.getLinkType());
        map.put("created_at", link.getCreatedAt());
        return map;
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

    private static String blankToEmpty(String value) {
        return value != null ? value : "";
    }
}
