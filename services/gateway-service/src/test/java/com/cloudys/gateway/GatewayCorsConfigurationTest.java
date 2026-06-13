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
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins"))
                .isNotNull();
    }

    @Test
    @DisplayName("should allow all origins and methods")
    void shouldAllowAllOriginsAndMethods() {
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-methods"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-headers"))
                .isEqualTo("*");
        assertThat(environment.getProperty("spring.cloud.gateway.globalcors.cors-configurations.[/**].allow-credentials"))
                .isEqualTo("true");
    }
}
