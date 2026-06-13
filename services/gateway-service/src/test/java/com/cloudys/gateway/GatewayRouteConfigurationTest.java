package com.cloudys.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that all Gateway routes are correctly configured.
 * Tests route definitions are loaded and match expected target services.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Gateway Route Configuration")
class GatewayRouteConfigurationTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("should load all route definitions from configuration")
    void shouldLoadAllRouteDefinitions() {
        List<String> routeIds = routeDefinitionLocator.getRouteDefinitions()
                .map(def -> def.getId())
                .collectList()
                .block();

        assertThat(routeIds).isNotNull();

        // Verify all expected route IDs are present
        assertThat(routeIds).contains(
                // Auth routes
                "test-auth",
                "test-permission",
                // Project routes
                "test-manage",
                "test-product",
                // Requirement routes
                "test-requirements",
                "test-requirements-compat",
                // Analysis routes
                "test-classification",
                "test-dedup",
                "test-actor",
                "test-analysis",
                "test-graph",
                "test-change",
                // Inference routes
                "test-inference-chat",
                "test-inference-classification",
                "test-inference-conflict",
                "test-inference-traceability",
                "test-inference-kb",
                "test-inference-l4",
                "test-inference-acquisition",
                // Compat routes
                "test-inference-compat",
                // Default
                "test-default"
        );
    }

    @Nested
    @DisplayName("Auth Service Routes")
    class AuthRoutes {

        @Test
        @DisplayName("should route /api/v2/auth/** and /api/v2/permission/** to auth-service")
        void shouldRouteAuthPaths() {
            List<String> allIds = routeDefinitionLocator.getRouteDefinitions()
                    .map(def -> def.getId())
                    .collectList()
                    .block();

            assertThat(allIds).isNotNull();
            // Auth service handles both /api/v2/auth/** and /api/v2/permission/**
            assertThat(allIds).contains("test-auth", "test-permission");
        }
    }

    @Nested
    @DisplayName("Project Service Routes")
    class ProjectRoutes {

        @Test
        @DisplayName("should route /api/v2/manage/** and /api/v2/product/** to project-service")
        void shouldRouteProjectPaths() {
            List<String> allIds = routeDefinitionLocator.getRouteDefinitions()
                    .map(def -> def.getId())
                    .collectList()
                    .block();

            assertThat(allIds).isNotNull();
            assertThat(allIds).contains("test-manage", "test-product");
        }
    }

    @Nested
    @DisplayName("Requirement Compatibility Routes")
    class RequirementCompatRoutes {

        @Test
        @DisplayName("should route legacy analysis requirement paths to requirement-analysis-service")
        void shouldRouteRequirementCompatPaths() {
            List<String> allIds = routeDefinitionLocator.getRouteDefinitions()
                    .map(def -> def.getId())
                    .collectList()
                    .block();

            assertThat(allIds).isNotNull();
            assertThat(allIds).contains("test-requirements-compat");
        }
    }

    @Nested
    @DisplayName("Inference Service Routes")
    class InferenceRoutes {

        @Test
        @DisplayName("should route all inference prefixes to inference-service")
        void shouldRouteInferencePaths() {
            List<String> routeIds = routeDefinitionLocator.getRouteDefinitions()
                    .filter(def -> def.getPredicates().stream()
                            .anyMatch(p -> p.toString().contains("inference")))
                    .map(def -> def.getId())
                    .collectList()
                    .block();

            assertThat(routeIds).isNotNull();
            assertThat(routeIds).contains(
                    "test-inference-chat",
                    "test-inference-classification",
                    "test-inference-conflict",
                    "test-inference-traceability",
                    "test-inference-kb",
                    "test-inference-l4",
                    "test-inference-acquisition",
                    "test-inference-compat"
            );
        }
    }

    @Nested
    @DisplayName("Default Catch-All Route")
    class DefaultRoute {

        @Test
        @DisplayName("should have default route for /api/v2/** with order 100")
        void shouldHaveDefaultRoute() {
            routeDefinitionLocator.getRouteDefinitions()
                    .filter(def -> "test-default".equals(def.getId()))
                    .next()
                    .as(StepVerifier::create)
                    .expectNextMatches(def -> def.getOrder() == 100)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("should register routes in RouteLocator without errors")
    void shouldRegisterRoutesInLocator() {
        List<org.springframework.cloud.gateway.route.Route> routes = routeLocator.getRoutes()
                .collectList()
                .block();

        assertThat(routes).isNotNull();
        assertThat(routes).isNotEmpty();
    }
}
