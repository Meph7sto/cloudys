package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.hasSize;
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
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DedupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementL123Repository l123Repository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        l123Repository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);

        RequirementL123 req = new RequirementL123();
        req.setReqId("req-1");
        req.setSessionId("session-1");
        req.setLevel("L2");
        req.setText("The system shall respond within 2 seconds");
        req.setFingerprint("fp-test-1");
        req.setCreatedAt(Instant.now());
        l123Repository.save(req);
    }

    @Test
    @DisplayName("detect duplicates finds matching fingerprints")
    void detectDuplicates() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "requirementTexts", List.of("The system shall respond within 2 seconds")
        ));

        mockMvc.perform(post("/api/v2/dedup/detect")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")))
                .andExpect(jsonPath("$.total", is(1)));
    }

    @Test
    @DisplayName("get dedup results for session with data")
    void getDedupResults() throws Exception {
        mockMvc.perform(get("/api/v2/dedup/sessions/session-1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")))
                .andExpect(jsonPath("$.total_requirements", is(1)));
    }

    @Test
    @DisplayName("get dedup results for empty session returns 404")
    void getDedupResultsNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/dedup/sessions/nonexistent/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
