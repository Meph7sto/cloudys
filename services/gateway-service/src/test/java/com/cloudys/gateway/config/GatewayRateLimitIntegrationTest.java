package com.cloudys.gateway.config;

import com.cloudys.common.core.constant.Constants;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Gateway rate limiting.
 * <p>
 * The test route {@code ratelimit-test} defined in {@code application-test.yml}
 * sends requests to {@code localhost:19999} (no server).  Requests that
 * survive the rate limiter get a connection-error 5xx; rate-limited
 * requests get 429.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Gateway Rate Limiting")
class GatewayRateLimitIntegrationTest {

    private static final String TEST_SECRET = "test-gateway-jwt-secret-minimum-32-chars";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    // ---- helpers -------------------------------------------------------

    private String tokenFor(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("user_id", userId)
                .claim("username", "rtest")
                .claim("role", "user")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }

    @BeforeEach
    void setUp() {
        rateLimiter.reset();
        rateLimiter.addRouteConfig("ratelimit-test", 5, 3);
    }

    // ---- tests ---------------------------------------------------------

    @Test
    @DisplayName("direct: 3 requests within burst pass, 4th denied")
    void directApiShouldWork() {
        String key = "user:d1";

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.isAllowed("ratelimit-test", key).block().isAllowed())
                    .as("direct #" + (i + 1))
                    .isTrue();
        }

        assertThat(rateLimiter.isAllowed("ratelimit-test", key).block().isAllowed())
                .as("direct #4 should be denied")
                .isFalse();
    }

    @Test
    @DisplayName("gateway: 1st request passes rate limit")
    void gatewayFirstRequestShouldPass() {
        String token = tokenFor("gw-single");

        webTestClient.get()
                .uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().is5xxServerError();  // passed limiter, downstream error
    }

    @Test
    @DisplayName("gateway: 3 requests pass, 4th gets 429")
    void gatewayBurstExhaustionShouldReturn429() {
        String token = tokenFor("gw-burst2");

        // First 3 — must all pass
        for (int i = 1; i <= 3; i++) {
            int num = i;
            webTestClient.get()
                    .uri("/api/v2/ratelimit-test/ok")
                    .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status)
                            .as("request #%d should NOT be 429", num)
                            .isNotEqualTo(429));
        }

        // 4th — must be rate-limited
        webTestClient.get()
                .uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("gateway: per-user isolation")
    void gatewayPerUserIsolation() {
        String tokenA = tokenFor("gw-iso-a2");
        String tokenB = tokenFor("gw-iso-b2");

        // Exhaust user A
        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                    .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + tokenA)
                    .exchange()
                    .expectStatus().value(s -> assertThat(s).isNotEqualTo(429));
        }

        // User A now rate-limited
        webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + tokenA)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // User B still OK
        webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + tokenB)
                .exchange()
                .expectStatus().value(s -> assertThat(s).isNotEqualTo(429));
    }

    @Test
    @DisplayName("gateway: same user should have isolated budgets per route")
    void gatewayRouteIsolationPerUser() {
        String token = tokenFor("gw-cross-route");

        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                    .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                    .exchange()
                    .expectStatus().value(s -> assertThat(s).isNotEqualTo(429));
        }

        webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        webTestClient.get().uri("/api/v2/manage/health-check")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().value(s -> assertThat(s)
                        .as("same user on another route should not inherit ratelimit-test exhaustion")
                        .isNotEqualTo(429));
    }

    @Test
    @DisplayName("gateway: bucket refills after wait")
    void gatewayRefillAfterWait() throws Exception {
        String token = tokenFor("gw-refill2");

        // Exhaust
        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                    .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                    .exchange()
                    .expectStatus().value(s -> assertThat(s).isNotEqualTo(429));
        }

        // Rate-limited
        webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Wait for refill
        Thread.sleep(300);

        // Should pass again
        webTestClient.get().uri("/api/v2/ratelimit-test/ok")
                .header(Constants.HEADER_AUTHORIZATION, Constants.TOKEN_PREFIX + token)
                .exchange()
                .expectStatus().value(s -> assertThat(s).isNotEqualTo(429));
    }
}
