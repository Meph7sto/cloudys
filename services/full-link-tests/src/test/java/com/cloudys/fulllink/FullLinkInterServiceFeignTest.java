package com.cloudys.fulllink;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.project.repository.ProjectRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-link test verifying inter-service Feign client communication.
 * Tests that services can call each other's APIs when both are running
 * in the same combined context with a shared database.
 */
@DisplayName("Full-Link Inter-Service Feign Communication")
class FullLinkInterServiceFeignTest extends FullLinkBaseTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private String authToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        // Register admin and create a project
        Map<String, Object> adminBody = Map.of(
                "username", "feign_admin",
                "password", "FeignAdmin123!",
                "display_name", "Feign Admin",
                "role", "super_admin"
        );
        restTemplate.postForEntity(authUrl("/register"), adminBody, Map.class);

        Map<String, Object> loginBody = Map.of(
                "username", "feign_admin",
                "password", "FeignAdmin123!"
        );
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                authUrl("/login"), loginBody, Map.class);
        authToken = (String) loginResp.getBody().get("token");

        // Create product and project
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        Map<String, Object> prodBody = Map.of("name", "Feign Test Product", "description", "test");
        ResponseEntity<Map> prodResp = restTemplate.exchange(
                productUrl("/products"), HttpMethod.POST,
                new HttpEntity<>(prodBody, headers), Map.class);
        String pid = (String) prodResp.getBody().get("product_id");

        Map<String, Object> projBody = Map.of("name", "Feign Test Project", "description", "test",
                "product_id", pid);
        ResponseEntity<Map> projResp = restTemplate.exchange(
                manageUrl("/projects"), HttpMethod.POST,
                new HttpEntity<>(projBody, headers), Map.class);
        projectId = (String) projResp.getBody().get("project_id");
    }

    @Test
    @DisplayName("should create member via permission endpoint and verify in database")
    void shouldAddProjectMember() {
        // First register another user
        Map<String, Object> memberBody = Map.of(
                "username", "feign_member",
                "password", "FeignMember123!",
                "display_name", "Feign Member",
                "role", "member"
        );
        ResponseEntity<Map> memberResp = restTemplate.postForEntity(
                authUrl("/register"), memberBody, Map.class);
        String memberId = (String) memberResp.getBody().get("user_id");

        // Add member to project via permission endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        Map<String, Object> addMemberBody = Map.of(
                "user_id", memberId,
                "role", "DEV"
        );

        ResponseEntity<Map> addResp = restTemplate.exchange(
                permissionUrl("/projects/" + projectId + "/members"),
                HttpMethod.POST,
                new HttpEntity<>(addMemberBody, headers),
                Map.class);

        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify member exists in the database
        assertThat(authUserRepository.findById(memberId)).isPresent();
    }

    @Test
    @DisplayName("should get permission context for a user on a project")
    void shouldGetPermissionContext() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> contextResp = restTemplate.exchange(
                permissionUrl("/projects/" + projectId + "/context"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(contextResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(contextResp.getBody()).isNotNull();
        // Permission context should contain user info and access flags
        assertThat(contextResp.getBody().get("user_id")).isNotNull();
        assertThat(contextResp.getBody().get("can_access")).isIn(true, false);
    }

    @Test
    @DisplayName("should verify auth-service database is consistent with API responses")
    void shouldHaveConsistentDataBetweenApiAndDatabase() {
        // Verify that the admin user exists in the database
        var adminUser = authUserRepository.findByUsername("feign_admin");
        assertThat(adminUser).isPresent();
        assertThat(adminUser.get().getUsername()).isEqualTo("feign_admin");

        // Verify the project exists in both API and DB
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        ResponseEntity<Map> apiResp = restTemplate.exchange(
                manageUrl("/projects/" + projectId),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        assertThat(apiResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var dbProject = projectRepository.findById(projectId);
        assertThat(dbProject).isPresent();
        assertThat(apiResp.getBody().get("name")).isEqualTo(dbProject.get().getName());
    }
}
