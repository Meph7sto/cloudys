package com.cloudys.fulllink;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full-link integration tests.
 * Provides a shared PostgreSQL Testcontainers instance and common test utilities.
 *
 * Starts all service modules in a single Spring context with real PostgreSQL.
 */
@SpringBootTest(
    classes = FullLinkBaseTest.FullLinkTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
public abstract class FullLinkBaseTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cloudys_test")
            .withUsername("cloudys_test")
            .withPassword("cloudys_test");

    @LocalServerPort
    protected int port;

    protected final TestRestTemplate restTemplate = new TestRestTemplate();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void setUpBase() {
        // Ensure container is running (may be shared across test classes)
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected String authUrl(String path) {
        return baseUrl() + "/api/v2/auth" + path;
    }

    protected String permissionUrl(String path) {
        return baseUrl() + "/api/v2/permission" + path;
    }

    protected String manageUrl(String path) {
        return baseUrl() + "/api/v2/manage" + path;
    }

    protected String productUrl(String path) {
        return baseUrl() + "/api/v2/product" + path;
    }

    protected String requirementsUrl(String path) {
        return baseUrl() + "/api/v2/requirements" + path;
    }

    protected String analysisUrl(String path) {
        return baseUrl() + "/api/v2/analysis" + path;
    }

    /**
     * Combined Spring Boot configuration that scans all service packages
     * plus common modules for JPA entities and shared beans.
     */
    @Configuration
    @ComponentScan(basePackages = {
        // Common modules
        "com.cloudys.common.core",
        "com.cloudys.common.dto",
        "com.cloudys.common.security",
        // Business services
        "com.cloudys.auth",
        "com.cloudys.project",
        "com.cloudys.requirement",
        "com.cloudys.requirementanalysis"
    })
    public static class FullLinkTestConfig {
    }
}
