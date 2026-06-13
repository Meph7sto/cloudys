package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirementanalysis.entity.RequirementGraphRelation;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.TraceAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementChangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementGraphRelationRepository graphRepository;
    @Autowired
    private TraceAnalysisRepository traceRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        traceRepository.deleteAll();
        graphRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);

        RequirementGraphRelation rel = new RequirementGraphRelation();
        rel.setSnapshotId("snap-1");
        rel.setRunId("run-1");
        rel.setProjectId("proj-1");
        rel.setSessionId("session-1");
        rel.setRelationMode("traceability");
        rel.setSourceNodeId("node-1");
        rel.setTargetNodeId("node-2");
        rel.setSourceReqId("req-1");
        rel.setTargetReqId("req-2");
        rel.setRelationType("depends_on");
        rel.setSourceKind("l3");
        rel.setWeight(0.8);
        rel.setIsActive(true);
        rel.setCreatedAt(Instant.now());
        graphRepository.save(rel);
    }

    @Test
    @DisplayName("analyze change impact")
    void analyzeChange() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "projectId", "proj-1",
                "changedReqIds", List.of("req-1")
        ));

        mockMvc.perform(post("/api/v2/requirement-change/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis_id", notNullValue()))
                .andExpect(jsonPath("$.session_id", is("session-1")));
    }

    @Test
    @DisplayName("get change history for session")
    void getChangeHistory() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "projectId", "proj-1",
                "changedReqIds", List.of("req-1")
        ));

        mockMvc.perform(post("/api/v2/requirement-change/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/requirement-change/history/session-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")));
    }
}
