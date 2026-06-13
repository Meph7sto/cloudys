package com.cloudys.project.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.project.client.PermissionServiceClient;
import com.cloudys.project.client.RequirementServiceClient;
import com.cloudys.project.dto.BindProjectRequest;
import com.cloudys.project.dto.CreateProductRequest;
import com.cloudys.project.dto.CreateProjectRequest;
import com.cloudys.project.dto.UpdateProductRequest;
import com.cloudys.project.entity.Milestone;
import com.cloudys.project.entity.Product;
import com.cloudys.project.entity.Project;
import com.cloudys.project.repository.MilestoneRepository;
import com.cloudys.project.repository.ProductRepository;
import com.cloudys.project.repository.ProjectRepository;
import com.cloudys.project.util.SecurityUtils;

@Service
public class ProductService {

    private static final List<String> VALID_STATUSES = List.of("active", "archived");

    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectManagementService projectService;
    private final RequirementServiceClient requirementServiceClient;
    private final PermissionServiceClient permissionServiceClient;
    private final AuditLogService auditLogService;

    public ProductService(ProductRepository productRepository,
                          ProjectRepository projectRepository,
                          MilestoneRepository milestoneRepository,
                          ProjectManagementService projectService,
                          RequirementServiceClient requirementServiceClient,
                          PermissionServiceClient permissionServiceClient,
                          AuditLogService auditLogService) {
        this.productRepository = productRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.projectService = projectService;
        this.requirementServiceClient = requirementServiceClient;
        this.permissionServiceClient = permissionServiceClient;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Map<String, Object> create(CreateProductRequest request) {
        requireAdmin();
        if (productRepository.findByName(request.name().trim()).isPresent()) {
            throw new ErrorResponse("Product name already exists", 400);
        }
        Product product = new Product();
        product.setProductId("prod-" + UUID.randomUUID());
        product.setName(request.name().trim());
        product.setDescription(defaultText(request.description()));
        product.setRoadmap(defaultText(request.roadmap()));
        product.setVersion(defaultText(request.version()));
        product.setTags(JsonSupport.toJson(request.tags() != null ? request.tags() : List.of()));
        product.setCreatedBy(SecurityUtils.getCurrentUserId());
        Product saved = productRepository.save(product);
        auditLogService.record(null, saved.getProductId(), "product.create", "product", saved.getProductId(),
                Map.of("description", "创建产品 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(boolean includeArchived) {
        List<Product> products = includeArchived
                ? productRepository.findAll()
                : productRepository.findByStatus("active");
        return Map.of("products", products.stream().map(this::toMap).toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String productId) {
        return toMap(getProduct(productId));
    }

    @Transactional
    public Map<String, Object> update(String productId, UpdateProductRequest request) {
        Product product = getProduct(productId);
        if (request.name() != null && !request.name().isBlank()) {
            productRepository.findByName(request.name().trim())
                    .filter(existing -> !existing.getProductId().equals(productId))
                    .ifPresent(existing -> {
                        throw new ErrorResponse("Product name already exists", 400);
                    });
            product.setName(request.name().trim());
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.roadmap() != null) product.setRoadmap(request.roadmap());
        if (request.version() != null) product.setVersion(request.version());
        if (request.tags() != null) product.setTags(JsonSupport.toJson(request.tags()));
        if (request.status() != null) {
            validateStatus(request.status());
            product.setStatus(request.status());
        }
        Product saved = productRepository.save(product);
        auditLogService.record(null, saved.getProductId(), "product.update", "product", saved.getProductId(),
                Map.of("description", "更新产品 " + saved.getName(), "name", saved.getName()));
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> archive(String productId) {
        Product product = getProduct(productId);
        product.setStatus("archived");
        productRepository.save(product);
        auditLogService.record(null, product.getProductId(), "product.archive", "product", product.getProductId(),
                Map.of("description", "归档产品 " + product.getName(), "name", product.getName()));
        return Map.of("message", "产品已归档");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProjects(String productId) {
        getProduct(productId);
        return Map.of("projects", projectRepository.findByProductId(productId).stream()
                .map(projectService::toMap)
                .toList());
    }

    @Transactional
    public Map<String, Object> createProjectUnderProduct(String productId, CreateProjectRequest request) {
        getProduct(productId);
        return projectService.create(request, productId);
    }

    @Transactional
    public Map<String, Object> bindProject(String projectId, BindProjectRequest request) {
        String productId = request.productId();
        if (productId != null) {
            getProduct(productId);
        }
        return projectService.bindProduct(projectId, productId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview(String productId) {
        Product product = getProduct(productId);
        List<Project> projects = projectRepository.findByProductId(productId);
        List<Map<String, Object>> requirements = listRequirementRows(productId, false);
        List<Milestone> milestones = listMilestoneEntities(productId);
        long baselineCount = milestones.stream().filter(m -> Boolean.TRUE.equals(m.getIsBaseline())).count();

        return Map.of(
                "product_id", product.getProductId(),
                "stats", Map.of(
                        "projects", Map.of("total", projects.size()),
                        "requirements", Map.of("total", requirements.size()),
                        "milestones", Map.of(
                                "total", milestones.size(),
                                "baselines", baselineCount
                        )
                )
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRequirements(String productId, boolean includeDeleted) {
        getProduct(productId);
        return Map.of("requirements", listRequirementRows(productId, includeDeleted));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listMilestones(String productId) {
        getProduct(productId);
        List<Map<String, Object>> milestones = listMilestoneEntities(productId).stream()
                .map(milestone -> {
                    Map<String, Object> project = projectService.get(milestone.getProjectId());
                    Map<String, Object> map = new LinkedHashMap<>(projectService.getMilestone(milestone.getMilestoneId()));
                    map.put("project_name", project.get("name"));
                    return map;
                })
                .toList();
        return Map.of("milestones", milestones);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listBaselines(String productId) {
        getProduct(productId);
        List<Map<String, Object>> baselines = projectRepository.findByProductId(productId).stream()
                .flatMap(project -> permissionServiceClient.listProjectBaselines(project.getProjectId()).stream()
                        .map(baseline -> {
                            Map<String, Object> map = new LinkedHashMap<>(baseline);
                            map.put("project_name", project.getName());
                            map.putIfAbsent("name", "Baseline " + baseline.getOrDefault("version", ""));
                            map.putIfAbsent("milestone_id", "baseline-" + baseline.getOrDefault("id", UUID.randomUUID()));
                            return map;
                        }))
                .toList();
        return Map.of("baselines", baselines);
    }

    private List<Map<String, Object>> listRequirementRows(String productId, boolean includeDeleted) {
        return projectRepository.findByProductId(productId).stream()
                .flatMap(project -> {
                    Map<String, Object> response = requirementServiceClient.listRequirements(project.getProjectId(), false, includeDeleted);
                    if (response.containsKey("error")) {
                        return List.<Map<String, Object>>of().stream();
                    }
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getOrDefault("requirements", List.of());
                    return rows.stream().map(row -> decorateRequirementRow(row, project));
                })
                .toList();
    }

    private Map<String, Object> decorateRequirementRow(Map<String, Object> row, Project project) {
        Map<String, Object> map = new LinkedHashMap<>(row);
        map.put("project_name", project.getName());
        map.putIfAbsent("title", row.getOrDefault("title", row.getOrDefault("text", row.getOrDefault("description", ""))));
        return map;
    }

    private List<Milestone> listMilestoneEntities(String productId) {
        LinkedHashSet<String> projectIds = projectRepository.findByProductId(productId).stream()
                .map(Project::getProjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return projectIds.stream()
                .flatMap(projectId -> milestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream())
                .toList();
    }

    Product getProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ErrorResponse("Product not found", 404));
    }

    Map<String, Object> toMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("product_id", product.getProductId());
        map.put("name", product.getName());
        map.put("description", product.getDescription());
        map.put("status", product.getStatus());
        map.put("roadmap", product.getRoadmap());
        map.put("version", product.getVersion());
        map.put("tags", JsonSupport.toStringList(product.getTags()));
        map.put("created_by", product.getCreatedBy());
        map.put("created_at", product.getCreatedAt());
        map.put("updated_at", product.getUpdatedAt());
        return map;
    }

    private static void requireAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new ErrorResponse("Permission denied", 403);
        }
    }

    private static void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ErrorResponse("status 必须是 active 或 archived", 400);
        }
    }

    private static String defaultText(String value) {
        return value != null ? value : "";
    }
}
