package com.cloudys.project.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.project.client.PermissionServiceClient;
import com.cloudys.project.client.RequirementServiceClient;
import com.cloudys.project.repository.BranchRepository;
import com.cloudys.project.repository.MilestoneRepository;
import com.cloudys.project.repository.ProductRepository;
import com.cloudys.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RequirementServiceClient requirementServiceClient;

    @MockBean
    private PermissionServiceClient permissionServiceClient;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        branchRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        productRepository.deleteAll();
        adminToken = tokenProvider.generateToken("u-admin", "admin", "super_admin", null);
        memberToken = tokenProvider.generateToken("u-member", "member", "member", null);

        org.mockito.Mockito.when(requirementServiceClient.listRequirements(org.mockito.ArgumentMatchers.anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String projectId = invocation.getArgument(0, String.class);
                    return Map.of("requirements", new ArrayList<>(List.of(
                            Map.of(
                                    "req_id", "req-" + projectId,
                                    "project_id", projectId,
                                    "title", "Requirement for " + projectId,
                                    "requirement_type", "top_level",
                                    "status", "draft",
                                    "priority", "medium",
                                    "test_case_links", List.of()
                            )
                    )));
                });
        org.mockito.Mockito.when(requirementServiceClient.createRequirement(eq("proj-x"), anyMap()))
                .thenReturn(Map.of("req_id", "req-created", "project_id", "proj-x", "title", "Created requirement"));
        org.mockito.Mockito.when(requirementServiceClient.updateRequirement(eq("req-created"), anyMap()))
                .thenReturn(Map.of("req_id", "req-created", "project_id", "proj-x", "title", "Updated requirement"));
        org.mockito.Mockito.when(requirementServiceClient.bulkUpdateStatus(anyMap()))
                .thenReturn(Map.of("updated", 1, "skipped", 0));
        org.mockito.Mockito.when(requirementServiceClient.listProjectDefects(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Map.of("defects", List.of(
                        Map.of(
                                "defect_id", "def-1",
                                "project_id", "proj-x",
                                "requirement_id", "req-created",
                                "title", "Example defect",
                                "severity", "critical",
                                "status", "open",
                                "current_assignee", "u-dev"
                        )
                )));
        org.mockito.Mockito.when(requirementServiceClient.listRequirementDefects(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Map.of("defects", List.of()));
        org.mockito.Mockito.when(requirementServiceClient.createDefect(eq("proj-x"), anyMap()))
                .thenReturn(Map.of("defect_id", "def-created", "project_id", "proj-x", "title", "Created defect"));
        org.mockito.Mockito.when(requirementServiceClient.updateDefect(eq("def-created"), anyMap()))
                .thenReturn(Map.of("defect_id", "def-created", "project_id", "proj-x", "title", "Updated defect"));
        org.mockito.Mockito.when(requirementServiceClient.deleteDefect(eq("def-created")))
                .thenReturn(Map.of("message", "缺陷已删除"));
        org.mockito.Mockito.when(requirementServiceClient.importRequirements(eq("proj-x"), anyMap()))
                .thenReturn(Map.of("inserted", 2, "inserted_l123", 1, "inserted_l4", 1));
        org.mockito.Mockito.when(requirementServiceClient.moveRequirement(eq("req-created"), anyMap()))
                .thenReturn(Map.of("message", "需求已移动"));
        org.mockito.Mockito.when(requirementServiceClient.deleteRequirement(eq("req-created"), eq(false)))
                .thenReturn(Map.of("deleted", 1));

        org.mockito.Mockito.when(permissionServiceClient.listProjectBaselines(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(Map.of("id", 1L, "project_id", "proj-x", "version", "v1.0", "created_at", "2026-06-12T00:00:00Z")));
    }

    @Test
    @DisplayName("product and project CRUD paths match frontend contracts")
    void productAndProjectCrud() throws Exception {
        String productId = createProduct("Product A");

        mockMvc.perform(get("/api/v2/product/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].product_id", is(productId)));

        mockMvc.perform(patch("/api/v2/product/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", "1.1.0", "tags", List.of("core")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is("1.1.0")))
                .andExpect(jsonPath("$.tags[0]", is("core")));

        String projectId = createProjectUnderProduct(productId, "Project A");

        mockMvc.perform(get("/api/v2/product/products/" + productId + "/projects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].project_id", is(projectId)));

        mockMvc.perform(patch("/api/v2/manage/projects/" + projectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("current_session_id", "session-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_session_id", is("session-1")));

        mockMvc.perform(delete("/api/v2/manage/projects/" + projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("归档")));
    }

    @Test
    @DisplayName("milestone and branch baseline flow works")
    void milestoneAndBranchFlow() throws Exception {
        String productId = createProduct("Product B");
        String projectId = createProjectUnderProduct(productId, "Project B");

        MvcResult milestoneResult = mockMvc.perform(post("/api/v2/manage/projects/" + projectId + "/milestones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "M1",
                                "milestone_type", "regular",
                                "tags", List.of("release")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestone_id", notNullValue()))
                .andExpect(jsonPath("$.tags[0]", is("release")))
                .andReturn();
        String milestoneId = read(milestoneResult).get("milestone_id").asText();

        mockMvc.perform(post("/api/v2/manage/milestones/" + milestoneId + "/baseline")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_baseline", is(true)))
                .andExpect(jsonPath("$.milestone_type", is("baseline")));

        mockMvc.perform(post("/api/v2/manage/projects/" + projectId + "/branches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "feature-x",
                                "base_milestone_id", milestoneId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branch_id", notNullValue()))
                .andExpect(jsonPath("$.base_milestone_id", is(milestoneId)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/branches")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branches", hasSize(1)))
                .andExpect(jsonPath("$.branches[0].name", is("feature-x")));
    }

    @Test
    @DisplayName("member cannot create products")
    void memberCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v2/product/products")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Denied Product"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", is("Permission denied")));
    }

    @Test
    @DisplayName("blank project name is rejected by bean validation")
    void blankProjectNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/v2/manage/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "   ", "description", "desc"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail[0].field", is("name")));
    }

    @Test
    @DisplayName("list milestones supports legacy JSON rows and dashboard contract")
    void listMilestonesSupportsLegacyJsonRows() throws Exception {
        String productId = createProduct("Product Legacy Milestone");
        String projectId = createProjectUnderProduct(productId, "Project Legacy Milestone");
        Instant createdAt = Instant.parse("2026-06-13T08:30:00Z");

        jdbcTemplate.update("""
                        insert into manage_milestones (
                            milestone_id, project_id, name, description, message, milestone_type,
                            is_baseline, sprint, version, tags, created_by, created_at, metadata
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, CAST(? AS JSON))
                        """,
                "ms-legacy-1",
                projectId,
                "Legacy Baseline",
                "Imported from legacy service",
                "legacy message",
                "baseline",
                true,
                "Sprint 9",
                "v1.2.3",
                "[\"release\", \"legacy\"]",
                "u-admin",
                Timestamp.from(createdAt),
                "{\"source\":\"python\",\"migrated\":true}");

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/milestones")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestones", hasSize(1)))
                .andExpect(jsonPath("$.milestones[0].milestone_id", is("ms-legacy-1")))
                .andExpect(jsonPath("$.milestones[0].project_id", is(projectId)))
                .andExpect(jsonPath("$.milestones[0].name", is("Legacy Baseline")))
                .andExpect(jsonPath("$.milestones[0].is_baseline", is(true)))
                .andExpect(jsonPath("$.milestones[0].tags[0]", is("release")))
                .andExpect(jsonPath("$.milestones[0].created_at", notNullValue()));
    }

    @Test
    @DisplayName("list audits supports legacy JSON payloads and dashboard contract")
    void listAuditsSupportsLegacyJsonRows() throws Exception {
        String productId = createProduct("Product Legacy Audit");
        String projectId = createProjectUnderProduct(productId, "Project Legacy Audit");
        Instant createdAt = Instant.parse("2026-06-13T09:45:00Z");

        jdbcTemplate.update("""
                        insert into manage_audit_logs (
                            log_id, project_id, product_id, actor, action, target_type, target_id, detail, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                        """,
                "audit-legacy-1",
                projectId,
                productId,
                "u-admin",
                "milestone.create",
                "milestone",
                "ms-legacy-1",
                "{\"description\":\"Legacy audit entry\",\"name\":\"Legacy Baseline\"}",
                Timestamp.from(createdAt));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs", hasSize(2)))
                .andExpect(jsonPath("$.audits", hasSize(2)))
                .andExpect(jsonPath("$.logs[1].description", is("Legacy audit entry")))
                .andExpect(jsonPath("$.logs[1].created_at", notNullValue()));
    }

    @Test
    @DisplayName("product aggregate and manage compatibility endpoints satisfy beta contracts")
    void compatibilityEndpointsWork() throws Exception {
        String productId = createProduct("Product Compat");
        String projectId = createProjectUnderProduct(productId, "Project Compat");

        mockMvc.perform(get("/api/v2/product/products/" + productId + "/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.projects.total", is(1)))
                .andExpect(jsonPath("$.stats.requirements.total", is(1)));

        mockMvc.perform(get("/api/v2/product/products/" + productId + "/requirements")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(1)))
                .andExpect(jsonPath("$.requirements[0].project_name", is("Project Compat")));

        mockMvc.perform(get("/api/v2/product/products/" + productId + "/baselines")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baselines", hasSize(1)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/requirements")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(1)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/defects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defects", hasSize(1)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/audits")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs", hasSize(1)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/traceability/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.requirement_count", is(1)));

        mockMvc.perform(get("/api/v2/manage/projects/" + projectId + "/traceability/coverage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverage.total", is(1)));
    }

    @Test
    @DisplayName("bind project should allow clearing product without internal error")
    void bindProjectAllowsClearingProduct() throws Exception {
        String productId = createProduct("Product Bind");
        String projectId = createProjectUnderProduct(productId, "Project Bind");

        mockMvc.perform(post("/api/v2/product/projects/" + projectId + "/bind")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id", is(projectId)))
                .andExpect(jsonPath("$.product_id").isEmpty());
    }

    private String createProduct(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/product/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "description", "desc",
                                "roadmap", "roadmap",
                                "version", "1.0.0",
                                "tags", List.of("alpha")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id", notNullValue()))
                .andReturn();
        return read(result).get("product_id").asText();
    }

    private String createProjectUnderProduct(String productId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/product/products/" + productId + "/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "description", "project desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id", notNullValue()))
                .andExpect(jsonPath("$.product_id", is(productId)))
                .andReturn();
        return read(result).get("project_id").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
