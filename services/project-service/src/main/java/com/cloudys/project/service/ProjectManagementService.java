package com.cloudys.project.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.project.dto.CreateBranchRequest;
import com.cloudys.project.dto.CreateMilestoneRequest;
import com.cloudys.project.dto.CreateProjectRequest;
import com.cloudys.project.dto.UpdateProjectRequest;
import com.cloudys.project.entity.Branch;
import com.cloudys.project.entity.Milestone;
import com.cloudys.project.entity.Project;
import com.cloudys.project.repository.BranchRepository;
import com.cloudys.project.repository.MilestoneRepository;
import com.cloudys.project.repository.ProjectRepository;
import com.cloudys.project.util.SecurityUtils;

@Service
public class ProjectManagementService {

    private static final List<String> VALID_PROJECT_STATUSES = List.of("active", "archived");
    private static final List<String> VALID_MILESTONE_TYPES = List.of("regular", "baseline", "branch", "merge");

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public ProjectManagementService(ProjectRepository projectRepository,
                                    MilestoneRepository milestoneRepository,
                                    BranchRepository branchRepository,
                                    AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> create(CreateProjectRequest request, String productId) {
        requireAdmin();
        if (projectRepository.findByName(request.name().trim()).isPresent()) {
            throw new ErrorResponse("Project name already exists", 400);
        }
        Project project = new Project();
        project.setProjectId("proj-" + UUID.randomUUID());
        project.setName(request.name().trim());
        project.setDescription(request.description() != null ? request.description() : "");
        project.setProductId(productId);
        project.setCreatedBy(SecurityUtils.getCurrentUserId());
        Project saved = projectRepository.save(project);
        auditLogService.record(saved.getProjectId(), saved.getProductId(), "project.create", "project", saved.getProjectId(),
                Map.of("description", "创建项目 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        return Map.of("projects", projectRepository.findByStatus("active").stream()
                .map(this::toMap)
                .toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String projectId) {
        return toMap(getProject(projectId));
    }

    @Transactional
    public Map<String, Object> update(String projectId, UpdateProjectRequest request) {
        Project project = getProject(projectId);
        if (request.name() != null && !request.name().isBlank()) {
            projectRepository.findByName(request.name().trim())
                    .filter(existing -> !existing.getProjectId().equals(projectId))
                    .ifPresent(existing -> {
                        throw new ErrorResponse("Project name already exists", 400);
                    });
            project.setName(request.name().trim());
        }
        if (request.description() != null) project.setDescription(request.description());
        if (request.status() != null) {
            validateProjectStatus(request.status());
            project.setStatus(request.status());
        }
        if (request.productId() != null) project.setProductId(request.productId());
        if (request.currentSessionId() != null) project.setCurrentSessionId(request.currentSessionId());
        Project saved = projectRepository.save(project);
        auditLogService.record(saved.getProjectId(), saved.getProductId(), "project.update", "project", saved.getProjectId(),
                Map.of("description", "更新项目 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> archive(String projectId) {
        Project project = getProject(projectId);
        project.setStatus("archived");
        projectRepository.save(project);
        auditLogService.record(project.getProjectId(), project.getProductId(), "project.archive", "project", project.getProjectId(),
                Map.of("description", "归档项目 " + project.getName(), "name", project.getName()));
        return Map.of("message", "项目已归档");
    }

    @Transactional
    public Map<String, Object> bindProduct(String projectId, String productId) {
        Project project = getProject(projectId);
        project.setProductId(productId);
        Project saved = projectRepository.save(project);
        auditLogService.record(saved.getProjectId(), saved.getProductId(), "project.bind_product", "project", saved.getProjectId(),
                Map.of("description", "绑定项目到产品", "product_id", productId));
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> createMilestone(String projectId, CreateMilestoneRequest request) {
        getProject(projectId);
        String type = request.milestoneType() != null ? request.milestoneType() : "regular";
        if (!VALID_MILESTONE_TYPES.contains(type)) {
            throw new ErrorResponse("milestone_type 无效", 400);
        }
        Milestone milestone = new Milestone();
        milestone.setMilestoneId("ms-" + UUID.randomUUID());
        milestone.setProjectId(projectId);
        milestone.setName(request.name().trim());
        milestone.setDescription(request.description());
        milestone.setMessage(request.message());
        milestone.setMilestoneType(type);
        milestone.setIsBaseline(request.isBaseline() != null ? request.isBaseline() : "baseline".equals(type));
        milestone.setSprint(request.sprint());
        milestone.setVersion(request.version());
        milestone.setTags(JsonSupport.toJson(request.tags() != null ? request.tags() : List.of()));
        milestone.setMetadata(JsonSupport.toJson(request.metadata()));
        milestone.setCreatedBy(SecurityUtils.getCurrentUserId());
        Milestone saved = milestoneRepository.save(milestone);
        auditLogService.record(projectId, getProject(projectId).getProductId(), "milestone.create", "milestone", saved.getMilestoneId(),
                Map.of("description", "创建里程碑 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listMilestones(String projectId) {
        getProject(projectId);
        return Map.of("milestones", milestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toMap)
                .toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMilestone(String milestoneId) {
        return toMap(getMilestoneEntity(milestoneId));
    }

    @Transactional
    public Map<String, Object> markBaseline(String milestoneId) {
        Milestone milestone = getMilestoneEntity(milestoneId);
        milestone.setIsBaseline(true);
        milestone.setMilestoneType("baseline");
        Milestone saved = milestoneRepository.save(milestone);
        auditLogService.record(saved.getProjectId(), getProject(saved.getProjectId()).getProductId(), "milestone.baseline", "milestone",
                saved.getMilestoneId(), Map.of("description", "标记基线 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> createBranch(String projectId, CreateBranchRequest request) {
        getProject(projectId);
        getMilestoneEntity(request.baseMilestoneId());
        Branch branch = new Branch();
        branch.setBranchId("br-" + UUID.randomUUID());
        branch.setProjectId(projectId);
        branch.setBaseMilestoneId(request.baseMilestoneId());
        branch.setName(request.name().trim());
        branch.setMetadata(JsonSupport.toJson(request.metadata()));
        branch.setCreatedBy(SecurityUtils.getCurrentUserId());
        Branch saved = branchRepository.save(branch);
        auditLogService.record(projectId, getProject(projectId).getProductId(), "branch.create", "branch", saved.getBranchId(),
                Map.of("description", "创建分支 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listBranches(String projectId) {
        getProject(projectId);
        return Map.of("branches", branchRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toMap)
                .toList());
    }

    Project getProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ErrorResponse("Project not found", 404));
    }

    public Map<String, Object> toMap(Project project) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("project_id", project.getProjectId());
        map.put("name", project.getName());
        map.put("description", project.getDescription());
        map.put("status", project.getStatus());
        map.put("product_id", project.getProductId());
        map.put("current_session_id", project.getCurrentSessionId());
        map.put("created_by", project.getCreatedBy());
        map.put("created_at", project.getCreatedAt());
        map.put("updated_at", project.getUpdatedAt());
        return map;
    }

    private Milestone getMilestoneEntity(String milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ErrorResponse("Milestone not found", 404));
    }

    private Map<String, Object> toMap(Milestone milestone) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("milestone_id", milestone.getMilestoneId());
        map.put("project_id", milestone.getProjectId());
        map.put("name", milestone.getName());
        map.put("description", milestone.getDescription());
        map.put("message", milestone.getMessage());
        map.put("milestone_type", milestone.getMilestoneType());
        map.put("is_baseline", milestone.getIsBaseline());
        map.put("sprint", milestone.getSprint());
        map.put("version", milestone.getVersion());
        map.put("tags", JsonSupport.toStringList(milestone.getTags()));
        map.put("created_by", milestone.getCreatedBy());
        map.put("created_at", milestone.getCreatedAt());
        return map;
    }

    private Map<String, Object> toMap(Branch branch) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("branch_id", branch.getBranchId());
        map.put("project_id", branch.getProjectId());
        map.put("base_milestone_id", branch.getBaseMilestoneId());
        map.put("name", branch.getName());
        map.put("status", branch.getStatus());
        map.put("created_by", branch.getCreatedBy());
        map.put("created_at", branch.getCreatedAt());
        map.put("updated_at", branch.getUpdatedAt());
        return map;
    }

    private static void requireAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new ErrorResponse("Permission denied", 403);
        }
    }

    private static void validateProjectStatus(String status) {
        if (!VALID_PROJECT_STATUSES.contains(status)) {
            throw new ErrorResponse("status 必须是 active 或 archived", 400);
        }
    }
}
