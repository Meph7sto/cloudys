package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ConflictAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ContextRunRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;
import com.cloudys.requirementanalysis.repository.RequirementsAnalysisRunRepository;
import com.cloudys.requirementanalysis.repository.SpanLinkRepository;
import com.cloudys.requirementanalysis.repository.SpanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BetaCompatibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private SpanRepository spanRepository;
    @Autowired
    private SpanLinkRepository spanLinkRepository;
    @Autowired
    private ContextRunRepository contextRunRepository;
    @Autowired
    private RequirementL123Repository requirementL123Repository;
    @Autowired
    private LowLevelRequirementRepository lowLevelRequirementRepository;
    @Autowired
    private LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository;
    @Autowired
    private ClassificationAnalysisRepository classificationAnalysisRepository;
    @Autowired
    private ConflictAnalysisRepository conflictAnalysisRepository;
    @Autowired
    private RequirementGraphRelationRepository requirementGraphRelationRepository;
    @Autowired
    private RequirementsAnalysisRunRepository requirementsAnalysisRunRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        requirementGraphRelationRepository.deleteAll();
        requirementsAnalysisRunRepository.deleteAll();
        conflictAnalysisRepository.deleteAll();
        classificationAnalysisRepository.deleteAll();
        lowLevelRequirementLinkRepository.deleteAll();
        lowLevelRequirementRepository.deleteAll();
        requirementL123Repository.deleteAll();
        spanLinkRepository.deleteAll();
        contextRunRepository.deleteAll();
        spanRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-admin", "admin", "super_admin", null);
    }

    @Test
    @DisplayName("legacy beta analysis and requirements endpoints work end to end")
    void betaCompatibilityFlowWorks() throws Exception {
        mockMvc.perform(get("/api/v2/analysis/sample_transcript")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String ingestBody = json(Map.of(
                "transcript_text", "产品经理: 需要统一项目与需求管理\\n研发负责人: 系统应支持从会话抽取需求\\n测试负责人: 缺陷要关联需求"
        ));
        MvcResult ingestResult = mockMvc.perform(post("/api/v2/analysis/ingest_transcript/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestBody))
                .andExpect(status().isOk())
                .andReturn();
        String ingestContent = ingestResult.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(ingestContent).contains("session_id");
        String sessionId = extractSessionIdFromSse(ingestContent);

        String buildBody = json(Map.of("session_id", sessionId, "options", Map.of("window_size", 5)));
        MvcResult buildResult = mockMvc.perform(post("/api/v2/analysis/build_context/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBody))
                .andExpect(status().isOk())
                .andReturn();
        String contextRunId = extractFieldFromSse(buildResult.getResponse().getContentAsString(), "context_run_id");

        mockMvc.perform(get("/api/v2/analysis/context_runs")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v2/analysis/spans/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spans", hasSize(3)));

        mockMvc.perform(get("/api/v2/analysis/span_links/" + contextRunId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.links", hasSize(greaterThanOrEqualTo(1))));

        String extractBody = json(Map.of("session_id", sessionId, "context_run_id", contextRunId, "reset_before_extract", true));
        mockMvc.perform(post("/api/v2/requirements/extract_l123")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(3)));

        mockMvc.perform(post("/api/v2/requirements/extract_l123/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/requirements/session/" + sessionId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("per_page", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(3)));

        MvcResult requirementsResult = mockMvc.perform(get("/api/v2/requirements/session/" + sessionId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("per_page", "10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode extractedRequirements = read(requirementsResult).path("requirements");
        String topReqId1 = extractedRequirements.get(1).path("req_id").asText();
        String topReqText1 = extractedRequirements.get(1).path("text").asText();
        String topReqId2 = extractedRequirements.get(2).path("req_id").asText();
        String topReqText2 = extractedRequirements.get(2).path("text").asText();

        mockMvc.perform(get("/api/v2/requirements/stats")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.by_level.L1", is(1)));

        String l4Body = json(Map.of(
                "session_id", sessionId,
                "requirements", List.of(
                        Map.of("id", topReqId1, "text", topReqText1),
                        Map.of("id", topReqId2, "text", topReqText2)
                ),
                "force_regenerate", true
        ));
        mockMvc.perform(post("/api/v2/requirements/l4/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(l4Body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(2)));

        mockMvc.perform(get("/api/v2/requirements/l4/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirements", hasSize(2)));

        mockMvc.perform(get("/api/v2/requirements/l4/" + sessionId + "/exists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists", is(true)));

        mockMvc.perform(post("/api/v2/trace-by-mapping")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("session_id", sessionId, "save_to_db", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relations", hasSize(2)));

        mockMvc.perform(get("/api/v2/trace/latest")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relations", hasSize(2)));

        mockMvc.perform(post("/api/v2/conflict/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "session_id", sessionId,
                                "requirements", List.of(
                                        Map.of("requirement_id", "a", "text", "系统必须记录所有操作"),
                                        Map.of("requirement_id", "b", "text", "系统禁止记录任何操作"),
                                        Map.of("requirement_id", "c", "text", "缺陷需要可追踪")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.pairs_evaluated", is(3)));

        mockMvc.perform(get("/api/v2/conflict/latest")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(3)));

        classificationAnalysisRepository.deleteAll();
        mockMvc.perform(get("/api/v2/classification/latest")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", is(0)));

        mockMvc.perform(post("/api/v2/classification/predict-texts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "session_id", sessionId,
                                "requirements", List.of("系统必须记录所有操作", "缺陷需要可追踪")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is(sessionId)));

        mockMvc.perform(get("/api/v2/classification/latest")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", is(2)));

        String saveRunBody = json(Map.of(
                "session_id", sessionId,
                "project_id", "proj-1",
                "context_run_id", contextRunId,
                "high_level_requirements", List.of(Map.of("req_id", topReqId1, "text", topReqText1)),
                "low_level_requirements", List.of(Map.of("req_id", "l4-1", "text", "系统应支持从会话抽取需求 的实现细化要求")),
                "trace_result", Map.of("relations", List.of(Map.of("high_req_id", topReqId1, "low_req_id", "l4-1", "has_relation", true))),
                "conflict_result", Map.of("items", List.of()),
                "classification_result", Map.of("predictions", List.of("functional"), "label_distribution", Map.of("functional", 1), "total", 1),
                "meta", Map.of("source", "beta-test")
        ));
        MvcResult saveRunResult = mockMvc.perform(post("/api/v2/analysis/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRunBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis_run_id", notNullValue()))
                .andReturn();
        String analysisRunId = read(saveRunResult).at("/data/analysis_run_id").asText();

        mockMvc.perform(get("/api/v2/analysis/runs")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId)
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v2/analysis/runs/latest")
                        .header("Authorization", "Bearer " + token)
                        .param("session_id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis_run_id", is(analysisRunId)));

        mockMvc.perform(get("/api/v2/analysis/runs/" + analysisRunId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis_run_id", is(analysisRunId)));

        mockMvc.perform(delete("/api/v2/requirements/l4/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", is(2)));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String extractSessionIdFromSse(String content) {
        return extractFieldFromSse(content, "session_id");
    }

    private String extractFieldFromSse(String content, String field) {
        int idx = content.lastIndexOf(field);
        org.assertj.core.api.Assertions.assertThat(idx).isGreaterThanOrEqualTo(0);
        String tail = content.substring(idx);
        int colon = tail.indexOf(':');
        int quoteStart = tail.indexOf('"', colon);
        int quoteEnd = tail.indexOf('"', quoteStart + 1);
        return tail.substring(quoteStart + 1, quoteEnd);
    }
}
