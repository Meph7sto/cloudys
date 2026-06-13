package com.cloudys.apicompat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.cloudys.auth.AuthServiceApplication;
import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.auth.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that all auth-service API responses consistently use snake_case keys.
 * Critical for Vue 3 frontend compatibility with unchanged Python-original frontend code.
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Snake Case Response Compatibility")
class SnakeCaseResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthUserRepository userRepository;

    private static String uniqueUser(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Creates a pre-approved active admin user directly in the database. */
    private String createActiveUser(String username, String displayName) {
        String passwordHash = authService.hashPassword("Test123!");
        AuthUser user = new AuthUser();
        user.setUserId("uid-" + UUID.randomUUID().toString().substring(0, 12));
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setRole("super_admin");
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        userRepository.save(user);
        return username;
    }

    /** Login as given user, return Bearer token. */
    private String loginAndGetToken(String username) throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v2/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "Test123!"))))
                .andExpect(status().isOk());
        String body = result.andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }

    @Test
    @DisplayName("register response should use snake_case exclusively")
    void registerResponseShouldUseSnakeCase() throws Exception {
        String user = uniqueUser("snake_reg");
        mockMvc.perform(post("/api/v2/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", user, "password", "Test123!", "display_name", "Snake Test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.display_name").exists())
                .andExpect(jsonPath("$.registration_status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.registrationStatus").doesNotExist());
    }

    @Test
    @DisplayName("login response must not contain camelCase keys")
    void loginResponseMustNotContainCamelCase() throws Exception {
        String user = uniqueUser("camel");
        createActiveUser(user, "Camel Test");

        mockMvc.perform(post("/api/v2/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", user, "password", "Test123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.display_name").exists())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.displayName").doesNotExist());
    }

    @Test
    @DisplayName("profile response should use snake_case keys")
    void profileResponseShouldUseSnakeCase() throws Exception {
        String user = uniqueUser("profile");
        createActiveUser(user, "Profile User");
        String token = loginAndGetToken(user);

        mockMvc.perform(get("/api/v2/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.display_name").exists())
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.registration_status").exists())
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }
}
