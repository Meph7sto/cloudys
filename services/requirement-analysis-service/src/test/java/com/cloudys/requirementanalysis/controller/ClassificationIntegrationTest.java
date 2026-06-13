package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ClassificationAnalysisRepository classificationRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InferenceServiceClient inferenceServiceClient;

    private String token;

    @BeforeEach
    void setUp() {
        classificationRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);

        when(inferenceServiceClient.classifyTexts(any()))
                .thenReturn(Map.of(
                        "predictions", List.of("functional", "non-functional", "functional"),
                        "label_distribution", Map.of("functional", 2, "non-functional", 1),
                        "total", 3
                ));
    }

    @Test
    @DisplayName("classify endpoint calls inference and returns results")
    void classifyReturnsResults() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "requirements", List.of("The system shall respond within 2s.", "The UI should be accessible.", "Data must be encrypted.")
        ));

        mockMvc.perform(post("/api/v2/classification/classify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.session_id", is("session-1")))
                .andExpect(jsonPath("$.total", is(3)));
    }

    @Test
    @DisplayName("get classification results for existing session")
    void getResultsForSession() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sessionId", "session-1",
                "requirements", List.of("The system shall respond within 2s.")
        ));

        mockMvc.perform(post("/api/v2/classification/classify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/classification/sessions/session-1/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")));
    }

    @Test
    @DisplayName("get results for nonexistent session returns 404")
    void getResultsNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/classification/sessions/nonexistent/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
