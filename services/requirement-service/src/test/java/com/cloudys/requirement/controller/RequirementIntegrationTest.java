package com.cloudys.requirement.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirement.client.AuthServiceClient;
import com.cloudys.requirement.client.BaselineServiceClient;
import com.cloudys.requirement.client.ProjectServiceClient;
import com.cloudys.requirement.client.RequirementAnalysisClient;
import com.cloudys.requirement.dto.AnalysisImportResponse;
import com.cloudys.requirement.repository.DefectRepository;
import com.cloudys.requirement.repository.ManageRequirementRepository;
import com.cloudys.requirement.repository.RequirementTestLinkRepository;
import com.cloudys.requirement.repository.TestCaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ManageRequirementRepository requirementRepository;
    @Autowired
    private TestCaseRepository testCaseRepository;
    @Autowired
    private RequirementTestLinkRepository requirementTestLinkRepository;
    @Autowired
    private DefectRepository defectRepository;

    @MockBean
    private AuthServiceClient authServiceClient;
    @MockBean
    private ProjectServiceClient projectServiceClient;
    @MockBean
    private BaselineServiceClient baselineServiceClient;
    @MockBean
    private RequirementAnalysisClient requirementAnalysisClient;

    private String token;

    @BeforeEach
    void setUp() {
        requirementTestLinkRepository.deleteAll();
        defectRepository.deleteAll();
        testCaseRepository.deleteAll();
        requirementRepository.deleteAll();

        token = jwtTokenProvider.generateToken("u-admin", "admin", "super_admin", null);

        when(projectServiceClient.getProject(anyString())).thenAnswer(invocation ->
                Map.of("project_id", invocation.getArgument(0, String.class), "name", "project"));
        when(authServiceClient.getPermissionContext(anyString())).thenReturn(Map.of(
                "can_access", true,
                "can_edit", true,
                "permissions", List.of("view_requirement", "edit_requirement", "delete_requirement")
        ));
        when(baselineServiceClient.getBaseline(10L)).thenReturn(Map.of(
                "id", 10L,
                "project_id", "proj-1",
                "version", "v1"
        ));
    }

    @Test
    @DisplayName("validation rejects blank title")
    void validationRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v2/requirements/projects/proj-1/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requirement_type":"top_level","title":" "}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("requirement import, testcase binding, and baseline assignment work end-to-end")
    void phase4CoreFlowWorks() throws Exception {
        when(requirementAnalysisClient.exportSessionRequirements("session-1")).thenReturn(new AnalysisImportResponse(
                List.of(new AnalysisImportResponse.AnalysisRequirementRecord("top-src", "L2", "Top title")),
                List.of(new AnalysisImportResponse.AnalysisLowLevelRequirementRecord(
                        "low-src", "top-src", "Top title", "Low title", "service", List.of("ac"), "manual", Map.of("x", "y"))),
                List.of(new AnalysisImportResponse.AnalysisLowLevelLinkRecord("low-src", "top-src"))
        ));

        mockMvc.perform(post("/api/v2/requirements/projects/proj-1/requirements/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"session_id":"session-1","mapping_mode":"tree"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted", is(2)))
                .andExpect(jsonPath("$.stats.top_level_total", is(1)))
                .andExpect(jsonPath("$.stats.low_level_total", is(1)));

        String lowLevelReqId = requirementRepository.findByProjectIdAndDeletedFalse("proj-1").stream()
                .filter(req -> "low_level".equals(req.getRequirementType()))
                .findFirst()
                .orElseThrow()
                .getReqId();

        MvcResult testCaseResult = mockMvc.perform(post("/api/v2/requirements/projects/proj-1/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"TC-1","status":"active","description":"desc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.test_case_id", notNullValue()))
                .andReturn();
        String testCaseId = read(testCaseResult).get("test_case_id").asText();

        mockMvc.perform(post("/api/v2/requirements/" + lowLevelReqId + "/bind-testcase")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"test_case_id":"%s","link_type":"verification"}
                                """.formatted(testCaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.link.test_case_id", is(testCaseId)));

        mockMvc.perform(post("/api/v2/requirements/" + lowLevelReqId + "/baseline")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseline_id":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline_id", is(10)));

        mockMvc.perform(get("/api/v2/requirements/" + lowLevelReqId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseline_id", is(10)))
                .andExpect(jsonPath("$.test_case_links", hasSize(1)))
                .andExpect(jsonPath("$.test_case_links[0].test_case_id", is(testCaseId)));
    }

    @Test
    @DisplayName("member without edit rights gets forbidden")
    void noEditPermissionGetsForbidden() throws Exception {
        when(authServiceClient.getPermissionContext("proj-2")).thenReturn(Map.of(
                "can_access", true,
                "can_edit", false,
                "permissions", List.of("view_requirement")
        ));

        mockMvc.perform(post("/api/v2/requirements/projects/proj-2/requirements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requirement_type":"top_level","title":"Denied"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", is("无权修改当前项目需求")));
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
