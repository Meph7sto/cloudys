package com.cloudys.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that CORS configuration is properly loaded from application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Gateway CORS Configuration")
class GatewayCorsConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("should have CORS configuration in environment")
    void shouldHaveCorsConfigInEnvironment() {
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns"))
                .isNotNull();
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins"))
                .isNull();
    }

    @Test
    @DisplayName("should allow credentialed CORS with origin patterns")
    void shouldAllowCredentialedCorsWithOriginPatterns() {
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origin-patterns"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-headers"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allow-credentials"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("should expose configurable gateway rate-limit properties")
    void shouldExposeRateLimitProperties() {
        assertThat(environment.getProperty("gateway.rate-limit.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("gateway.rate-limit.default.replenish-rate")).isEqualTo("100");
        assertThat(environment.getProperty("gateway.rate-limit.default.burst-capacity")).isEqualTo("200");
        assertThat(environment.getProperty("gateway.rate-limit.routes.auth-service-auth.replenish-rate")).isEqualTo("10");
        assertThat(environment.getProperty("gateway.rate-limit.routes.auth-service-auth.burst-capacity")).isEqualTo("20");
        assertThat(environment.getProperty("gateway.rate-limit.routes.inference-service-chat.replenish-rate")).isEqualTo("20");
        assertThat(environment.getProperty("gateway.rate-limit.routes.inference-service-chat.burst-capacity")).isEqualTo("30");
    }
}
