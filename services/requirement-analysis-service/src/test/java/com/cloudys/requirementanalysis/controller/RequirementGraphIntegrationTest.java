package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
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
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementGraphIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementGraphRelationRepository graphRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
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
        rel.setConfidence(0.9);
        rel.setIsActive(true);
        rel.setCreatedAt(Instant.now());
        graphRepository.save(rel);
    }

    @Test
    @DisplayName("search active relations returns results")
    void searchRelations() throws Exception {
        mockMvc.perform(get("/api/v2/requirement-graph/relations")
                        .param("projectId", "proj-1")
                        .param("sessionId", "session-1")
                        .param("relationMode", "traceability")
                        .param("activeOnly", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relations").isArray())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("get snapshot by id")
    void getSnapshot() throws Exception {
        mockMvc.perform(get("/api/v2/requirement-graph/snapshots/snap-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot_id", is("snap-1")));
    }

    @Test
    @DisplayName("invalidate relations by snapshot")
    void invalidateBySnapshot() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "snapshotId", "snap-1",
                "reason", "test invalidation"
        ));

        mockMvc.perform(post("/api/v2/requirement-graph/invalidate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidated", is(true)));
    }

    @Test
    @DisplayName("get stats returns aggregate counts")
    void getStats() throws Exception {
        mockMvc.perform(get("/api/v2/requirement-graph/stats")
                        .param("projectId", "proj-1")
                        .param("sessionId", "session-1")
                        .param("relationMode", "traceability")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_relations", greaterThanOrEqualTo(1)));
    }
}
