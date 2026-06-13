package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.cloudys.requirementanalysis.repository.BundleItemRepository;
import com.cloudys.requirementanalysis.repository.BundleRepository;
import com.cloudys.requirementanalysis.repository.ContextRunRepository;
import com.cloudys.requirementanalysis.repository.SpanLinkRepository;
import com.cloudys.requirementanalysis.repository.SpanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContextControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private SpanRepository spanRepository;
    @Autowired
    private ContextRunRepository contextRunRepository;
    @Autowired
    private SpanLinkRepository spanLinkRepository;
    @Autowired
    private BundleRepository bundleRepository;
    @Autowired
    private BundleItemRepository bundleItemRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        bundleItemRepository.deleteAll();
        bundleRepository.deleteAll();
        spanLinkRepository.deleteAll();
        contextRunRepository.deleteAll();
        spanRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);
    }

    @Test
    @DisplayName("create span returns span_id and created_at")
    void createSpan() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1",
                "speaker", "Alice",
                "text", "The system shall support multi-tenancy.",
                "startMs", 0,
                "endMs", 5000
        ));

        mockMvc.perform(post("/api/v2/analysis/context/spans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.span_id", notNullValue()))
                .andExpect(jsonPath("$.created_at", notNullValue()));
    }

    @Test
    @DisplayName("list spans returns spans array")
    void listSpans() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1",
                "speaker", "Bob",
                "text", "Authentication should use OAuth2.",
                "startMs", 1000,
                "endMs", 8000
        ));

        mockMvc.perform(post("/api/v2/analysis/context/spans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/analysis/context/spans/session-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")))
                .andExpect(jsonPath("$.spans", hasSize(1)))
                .andExpect(jsonPath("$.spans[0].speaker", is("Bob")));
    }

    @Test
    @DisplayName("context run CRUD lifecycle")
    void contextRunLifecycle() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1",
                "optionsSnapshot", java.util.Map.of("window_size", 5)
        ));

        mockMvc.perform(post("/api/v2/analysis/context/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context_run_id", notNullValue()));

        mockMvc.perform(get("/api/v2/analysis/context/runs/session-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs", hasSize(1)));
    }

    @Test
    @DisplayName("bundle CRUD lifecycle")
    void bundleLifecycle() throws Exception {
        // Create span first
        String spanBody = objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1",
                "speaker", "Carol",
                "text", "The UI should be responsive.",
                "startMs", 0,
                "endMs", 3000
        ));
        String spanResponse = mockMvc.perform(post("/api/v2/analysis/context/spans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spanBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String spanId = objectMapper.readTree(spanResponse).get("span_id").asText();

        // Create context run
        String runBody = objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1"
        ));
        String runResponse = mockMvc.perform(post("/api/v2/analysis/context/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(runBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String runId = objectMapper.readTree(runResponse).get("context_run_id").asText();

        // Create bundle
        String bundleBody = objectMapper.writeValueAsString(java.util.Map.of(
                "contextRunId", runId,
                "sessionId", "session-1",
                "orderIndex", 0
        ));
        String bundleResponse = mockMvc.perform(post("/api/v2/analysis/context/bundles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bundleBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bundleId = objectMapper.readTree(bundleResponse).get("bundle_id").asText();

        // Add bundle item
        String itemBody = objectMapper.writeValueAsString(java.util.Map.of(
                "spanId", spanId,
                "spanRef", "ref-1",
                "orderIndex", 0
        ));
        mockMvc.perform(post("/api/v2/analysis/context/bundles/" + bundleId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));

        // List bundles with items
        mockMvc.perform(get("/api/v2/analysis/context/bundles/" + runId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundles", hasSize(1)))
                .andExpect(jsonPath("$.bundles[0].items", hasSize(1)));
    }

    @Test
    @DisplayName("unauthenticated request returns 401")
    void unauthenticatedFails() throws Exception {
        mockMvc.perform(get("/api/v2/analysis/context/spans/session-1"))
                .andExpect(status().isUnauthorized());
    }
}
