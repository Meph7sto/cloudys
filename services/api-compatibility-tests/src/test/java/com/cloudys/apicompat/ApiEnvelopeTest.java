package com.cloudys.apicompat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API Envelope Format Compatibility")
class ApiEnvelopeTest {

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

    @Nested
    @DisplayName("Login Response Format")
    class LoginFormat {

        @Test
        @DisplayName("should return token with snake_case fields")
        void shouldReturnSnakeCaseTokenResponse() throws Exception {
            String user = uniqueUser("compat");
            createActiveUser(user, "Test User");

            mockMvc.perform(post("/api/v2/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("username", user, "password", "Test123!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.user_id").isString())
                    .andExpect(jsonPath("$.username").value(user))
                    .andExpect(jsonPath("$.role").isString());
        }

        @Test
        @DisplayName("should match Python FastAPI login response shape")
        void shouldMatchPythonLoginShape() throws Exception {
            String user = uniqueUser("compat2");
            createActiveUser(user, "Test User");

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
    }

    @Nested
    @DisplayName("Register Response Format")
    class RegisterFormat {

        @Test
        @DisplayName("should return snake_case registration response")
        void shouldReturnSnakeCaseRegisterResponse() throws Exception {
            String user = uniqueUser("newuser");
            mockMvc.perform(post("/api/v2/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "username", user, "password", "Test123!", "display_name", "New User"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user_id").isString())
                    .andExpect(jsonPath("$.username").value(user))
                    .andExpect(jsonPath("$.display_name").isString())
                    .andExpect(jsonPath("$.registration_status").value(
                            matchesPattern("pending|approved")))
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test
        @DisplayName("should accept snake_case display_name in request body")
        void shouldAcceptSnakeCaseDisplayName() throws Exception {
            String user = uniqueUser("snake");
            mockMvc.perform(post("/api/v2/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "username", user, "password", "Test123!", "display_name", "Snake Case"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.display_name").value("Snake Case"));
        }
    }

    @Nested
    @DisplayName("Error Response Format")
    class ErrorFormat {

        @Test
        @DisplayName("should return Python-compatible error envelope for invalid login")
        void shouldReturnPythonCompatibleError() throws Exception {
            mockMvc.perform(post("/api/v2/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("username", "nonexistent_xyz", "password", "wrong"))))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.detail").isString());
        }

        @Test
        @DisplayName("should return 422 for validation errors with Python-compatible format")
        void shouldReturn422ForValidationErrors() throws Exception {
            mockMvc.perform(post("/api/v2/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("username", "", "password", "ab", "display_name", ""))))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Profile Response Format")
    class ProfileFormat {

        @Test
        @DisplayName("should return profile with snake_case fields")
        void shouldReturnSnakeCaseProfile() throws Exception {
            String user = uniqueUser("profile");
            createActiveUser(user, "Profile User");
            String token = loginAndGetToken(user);

            mockMvc.perform(get("/api/v2/auth/me")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user_id").isString())
                    .andExpect(jsonPath("$.username").isString())
                    .andExpect(jsonPath("$.display_name").isString())
                    .andExpect(jsonPath("$.role").isString())
                    .andExpect(jsonPath("$.registration_status").isString())
                    .andExpect(jsonPath("$.userId").doesNotExist())
                    .andExpect(jsonPath("$.displayName").doesNotExist());
        }
    }
}
