package com.cloudys.fulllink;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-link test for the project management flow.
 * Tests auth-service → project-service interaction with shared PostgreSQL.
 *
 * Flow: register admin → login → create product → create project → add milestone → list projects
 */
@DisplayName("Full-Link Project Flow")
class FullLinkProjectFlowTest extends FullLinkBaseTest {

    private String authToken;
    private String productId;

    /**
     * Register an admin user and login to get a token before each test class.
     */
    @BeforeEach
    void setUp() {
        // Register admin user
        Map<String, Object> adminBody = Map.of(
                "username", "project_admin",
                "password", "ProjectAdmin123!",
                "display_name", "Project Admin",
                "role", "super_admin"
        );
        restTemplate.postForEntity(authUrl("/register"), adminBody, Map.class);

        // Login
        Map<String, Object> loginBody = Map.of(
                "username", "project_admin",
                "password", "ProjectAdmin123!"
        );
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                authUrl("/login"), loginBody, Map.class);
        authToken = (String) loginResp.getBody().get("token");
    }

    @Nested
    @DisplayName("Product CRUD")
    class ProductCRUD {

        @Test
        @DisplayName("should create, list, and update a product")
        void shouldManageProductLifecycle() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            // Create product
            Map<String, Object> createBody = Map.of(
                    "name", "Test Product Alpha",
                    "description", "A test product for full-link testing"
            );

            ResponseEntity<Map> createResp = restTemplate.exchange(
                    productUrl("/products"),
                    HttpMethod.POST,
                    new HttpEntity<>(createBody, headers),
                    Map.class);

            assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(createResp.getBody()).isNotNull();
            assertThat(createResp.getBody().get("product_id")).isNotNull();
            assertThat(createResp.getBody().get("name")).isEqualTo("Test Product Alpha");
            productId = (String) createResp.getBody().get("product_id");

            // List products
            ResponseEntity<Map> listResp = restTemplate.exchange(
                    productUrl("/products"),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(listResp.getBody().get("products")).isNotNull();

            // Update product
            Map<String, Object> updateBody = Map.of(
                    "name", "Test Product Alpha v2",
                    "description", "Updated description"
            );
            ResponseEntity<Map> updateResp = restTemplate.exchange(
                    productUrl("/products/" + productId),
                    HttpMethod.PATCH,
                    new HttpEntity<>(updateBody, headers),
                    Map.class);
            assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(updateResp.getBody().get("name")).isEqualTo("Test Product Alpha v2");
        }
    }

    @Nested
    @DisplayName("Project CRUD with Milestones")
    class ProjectCRUD {

        @Test
        @DisplayName("should create project with milestones")
        void shouldCreateProjectWithMilestones() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authToken);

            // First create a product
            Map<String, Object> productBody = Map.of(
                    "name", "Project Product",
                    "description", "Product for project test"
            );
            ResponseEntity<Map> prodResp = restTemplate.exchange(
                    productUrl("/products"),
                    HttpMethod.POST,
                    new HttpEntity<>(productBody, headers),
                    Map.class);
            String pid = (String) prodResp.getBody().get("product_id");

            // Create project
            Map<String, Object> projectBody = Map.of(
                    "name", "Integration Test Project",
                    "description", "A project created during full-link testing",
                    "product_id", pid
            );

            ResponseEntity<Map> projectResp = restTemplate.exchange(
                    manageUrl("/projects"),
                    HttpMethod.POST,
                    new HttpEntity<>(projectBody, headers),
                    Map.class);

            assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(projectResp.getBody()).isNotNull();
            assertThat(projectResp.getBody().get("project_id")).isNotNull();
            assertThat(projectResp.getBody().get("name")).isEqualTo("Integration Test Project");

            String projectId = (String) projectResp.getBody().get("project_id");

            // Create milestone
            Map<String, Object> milestoneBody = Map.of(
                    "name", "Sprint 1",
                    "target_date", "2026-07-01"
            );

            ResponseEntity<Map> milestoneResp = restTemplate.exchange(
                    manageUrl("/projects/" + projectId + "/milestones"),
                    HttpMethod.POST,
                    new HttpEntity<>(milestoneBody, headers),
                    Map.class);

            assertThat(milestoneResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(milestoneResp.getBody().get("milestone_id")).isNotNull();

            // List projects — verify our project is there
            ResponseEntity<Map> listResp = restTemplate.exchange(
                    manageUrl("/projects"),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(listResp.getBody().get("projects")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Unauthenticated Access")
    class UnauthorizedAccess {

        @Test
        @DisplayName("should reject unauthenticated project creation")
        void shouldRejectUnauthenticatedCreate() {
            Map<String, Object> projectBody = Map.of(
                    "name", "Unauthorized Project",
                    "description", "Should fail"
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    manageUrl("/projects"), projectBody, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
