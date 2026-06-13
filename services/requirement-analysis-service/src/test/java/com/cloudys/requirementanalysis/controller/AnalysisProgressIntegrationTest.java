package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.client.RequirementServiceClient;
import com.cloudys.requirementanalysis.dto.RequirementImportRequest;
import com.cloudys.requirementanalysis.repository.RequirementsAnalysisRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisProgressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementsAnalysisRunRepository runRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InferenceServiceClient inferenceServiceClient;
    @MockBean
    private RequirementServiceClient requirementServiceClient;

    private String token;

    @BeforeEach
    void setUp() {
        runRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);
        when(inferenceServiceClient.extractRequirements(any())).thenReturn(Map.of(
                "requirements", java.util.List.of(
                        Map.of("level", "L3", "text", "The platform shall support export reports.", "fingerprint", "fp-1")
                )));
        when(inferenceServiceClient.generateL4(any())).thenReturn(Map.of(
                "text", "The reporting service shall generate downloadable PDF reports.",
                "component", "reporting",
                "acceptance_criteria", java.util.List.of("export succeeds"),
                "test_method", "manual"
        ));
        when(inferenceServiceClient.classifyTexts(any())).thenReturn(Map.of(
                "predictions", java.util.List.of("functional"),
                "label_distribution", Map.of("functional", 1)
        ));
        when(inferenceServiceClient.batchAnalyzeRelations(any())).thenReturn(Map.of("relations", java.util.List.of()));
        when(inferenceServiceClient.checkConflict(any())).thenReturn(Map.of("is_conflict", false));
        when(requirementServiceClient.importRequirements(eq("proj-1"), any(RequirementImportRequest.class)))
                .thenReturn(Map.of("inserted", 2, "mapping_mode", "tree"));
    }

    @Test
    @DisplayName("start analysis run returns run id")
    void startAnalysis() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "projectId", "proj-1",
                "contextRunId", "ctx-1"
        ));

        mockMvc.perform(post("/api/v2/analysis/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis_run_id", notNullValue()))
                .andExpect(jsonPath("$.status", is("RUNNING")));
    }

    @Test
    @DisplayName("SSE progress endpoint connects successfully")
    void sseProgressConnects() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "projectId", "proj-1",
                "contextRunId", "ctx-1"
        ));

        MvcResult result = mockMvc.perform(post("/api/v2/analysis/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String runId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("analysis_run_id").asText();

        mockMvc.perform(get("/api/v2/analysis/progress/" + runId)
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("analysis run imports generated requirements into requirement-service")
    void analysisRunImportsRequirements() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "projectId", "proj-1",
                "contextRunId", "ctx-1",
                "autoImport", true,
                "mappingMode", "tree"
        ));

        MvcResult result = mockMvc.perform(post("/api/v2/analysis/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String runId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("analysis_run_id").asText();

        verify(requirementServiceClient, timeout(2000).atLeastOnce())
                .importRequirements(eq("proj-1"), eq(new RequirementImportRequest("session-1", "tree")));

        org.assertj.core.api.Assertions.assertThat(runRepository.findById(runId)).isPresent();
    }
}
