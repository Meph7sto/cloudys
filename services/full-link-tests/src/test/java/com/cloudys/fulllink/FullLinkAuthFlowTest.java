package com.cloudys.fulllink;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-link test for the complete auth service flow.
 * Uses a real PostgreSQL database via Testcontainers.
 *
 * Flow: register → login → verify token → get profile → update profile → change password
 */
@DisplayName("Full-Link Auth Flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullLinkAuthFlowTest extends FullLinkBaseTest {

    private static String authToken;
    private static String registeredUserId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Register a new user")
    void shouldRegisterNewUser() {
        Map<String, Object> body = Map.of(
                "username", "fulllink_test_user",
                "password", "TestPass123!",
                "display_name", "Full Link Test User"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                authUrl("/register"), body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("user_id")).isNotNull();
        assertThat(response.getBody().get("username")).isEqualTo("fulllink_test_user");
        assertThat(response.getBody().get("registration_status")).isEqualTo("pending");

        registeredUserId = (String) response.getBody().get("user_id");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Login and receive JWT token")
    void shouldLoginAndReceiveToken() {
        // Admin needs to approve the user first — use admin login
        // For test purposes, the first user is super_admin
        // Actually, we test with whatever is available.
        // Since this is a fresh DB, register creates pending user.
        // We need to test login. Let's use a direct approach:
        // Register another user as admin, or directly test failed login first

        // Register a second user as admin (role=super_admin bypasses approval)
        Map<String, Object> adminBody = Map.of(
                "username", "admin_user",
                "password", "AdminPass123!",
                "display_name", "Admin User",
                "role", "super_admin"
        );
        restTemplate.postForEntity(authUrl("/register"), adminBody, Map.class);

        Map<String, Object> loginBody = Map.of(
                "username", "admin_user",
                "password", "AdminPass123!"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                authUrl("/login"), loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("token")).isNotNull();
        assertThat(response.getBody().get("user_id")).isNotNull();

        authToken = (String) response.getBody().get("token");
        registeredUserId = (String) response.getBody().get("user_id");

        // Verify token is a non-empty string
        assertThat(authToken).isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Verify token is valid and returns user info")
    void shouldVerifyToken() {
        assertThat(authToken).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                authUrl("/verify"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("user_id")).isEqualTo(registeredUserId);
        assertThat(response.getBody().get("username")).isEqualTo("admin_user");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Get current user profile")
    void shouldGetProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                authUrl("/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("user_id")).isEqualTo(registeredUserId);
        assertThat(response.getBody().get("username")).isEqualTo("admin_user");
        assertThat(response.getBody().get("display_name")).isEqualTo("Admin User");
        assertThat(response.getBody().get("role")).isIn("super_admin", "admin");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Update profile with valid data")
    void shouldUpdateProfile() {
        Map<String, Object> updateBody = Map.of(
                "display_name", "Updated Admin User",
                "avatar_url", "https://example.com/avatar.png"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                authUrl("/profile"),
                HttpMethod.PATCH,
                new HttpEntity<>(updateBody, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("display_name")).isEqualTo("Updated Admin User");
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Change password successfully")
    void shouldChangePassword() {
        Map<String, Object> changePwdBody = Map.of(
                "old_password", "AdminPass123!",
                "new_password", "NewAdminPass456!"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                authUrl("/profile/change-password"),
                HttpMethod.PUT,
                new HttpEntity<>(changePwdBody, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Verify we can login with new password
        Map<String, Object> loginBody = Map.of(
                "username", "admin_user",
                "password", "NewAdminPass456!"
        );
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                authUrl("/login"), loginBody, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Unauthenticated request to protected endpoint returns 401")
    void shouldRejectUnauthenticatedRequest() {
        // No auth header
        ResponseEntity<Map> response = restTemplate.getForEntity(
                authUrl("/me"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Invalid token returns 401")
    void shouldRejectInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token-value-12345");

        ResponseEntity<Map> response = restTemplate.exchange(
                authUrl("/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
