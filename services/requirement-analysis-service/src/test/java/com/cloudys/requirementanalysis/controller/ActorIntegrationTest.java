package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirementanalysis.repository.RequirementActorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementActorRepository actorRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        actorRepository.deleteAll();
        token = jwtTokenProvider.generateToken("u-test", "test", "user", null);
    }

    @Test
    @DisplayName("actor CRUD lifecycle")
    void actorLifecycle() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "requirementId", "req-1",
                "actorType", "human",
                "actorName", "Developer",
                "status", "idle"
        ));

        String response = mockMvc.perform(post("/api/v2/actor")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actor_id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String actorId = objectMapper.readTree(response).get("actor_id").asText();

        mockMvc.perform(get("/api/v2/actor/requirement/req-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actors", hasSize(1)));

        String updateBody = objectMapper.writeValueAsString(Map.of("status", "working"));
        mockMvc.perform(patch("/api/v2/actor/" + actorId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("working")));

        mockMvc.perform(delete("/api/v2/actor/" + actorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", is(true)));
    }

    @Test
    @DisplayName("update nonexistent actor returns 404")
    void updateNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("status", "working"));
        mockMvc.perform(patch("/api/v2/actor/nonexistent")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
