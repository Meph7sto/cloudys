package com.cloudys.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryRateLimiter}.
 * Verifies token-bucket behaviour directly without bringing up
 * a full Spring context.
 */
@DisplayName("InMemoryRateLimiter")
class InMemoryRateLimiterTest {

    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new InMemoryRateLimiter();
    }

    // ---- default config ------------------------------------------------

    @Test
    @DisplayName("should allow requests within default limit")
    void shouldAllowWithinDefaultLimit() {
        String routeId = "test-default";
        String key = "user:u1";

        // First request should be allowed (burst capacity 200)
        limiter.isAllowed(routeId, key)
                .as(StepVerifier::create)
                .assertNext(resp -> {
                    assertThat(resp.isAllowed()).isTrue();
                    assertThat(resp.getHeaders().get("X-RateLimit-Remaining"))
                            .isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should allow multiple requests up to burst capacity")
    void shouldAllowUpToBurstCapacity() {
        String routeId = "test-default";
        String key = "user:u1";

        // Default burst = 200, so 50 fast requests should all pass
        for (int i = 0; i < 50; i++) {
            int requestNum = i + 1;
            limiter.isAllowed(routeId, key)
                    .as(StepVerifier::create)
                    .assertNext(resp -> assertThat(resp.isAllowed())
                            .as("request #" + requestNum)
                            .isTrue())
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("should eventually deny after exhausting burst capacity")
    void shouldEventuallyDenyAfterExhaustion() {
        // Use a very small bucket to make exhaustion fast
        limiter.addRouteConfig("small", 1, 5);
        String key = "user:small";

        // Consume the entire burst of 5 — all should pass
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.isAllowed("small", key).block().isAllowed()).isTrue();
        }

        // 6th request should be denied (bucket empty, refill is 1/s)
        limiter.isAllowed("small", key)
                .as(StepVerifier::create)
                .assertNext(resp -> assertThat(resp.isAllowed()).isFalse())
                .verifyComplete();
    }

    // ---- route-specific config -----------------------------------------

    @Test
    @DisplayName("should use route-specific config when registered")
    void shouldUseRouteSpecificConfig() {
        limiter.addRouteConfig("strict", 3, 5);
        String key = "user:strict";

        // 5 tokens in burst
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.isAllowed("strict", key).block().isAllowed()).isTrue();
        }
        // 6th denied
        assertThat(limiter.isAllowed("strict", key).block().isAllowed()).isFalse();
    }

    @Test
    @DisplayName("should fall back to default config for unknown routes")
    void shouldFallbackToDefaultForUnknownRoute() {
        // Unknown route uses default (100 req/s, burst 200)
        limiter.isAllowed("no-such-route", "user:x")
                .as(StepVerifier::create)
                .assertNext(resp -> assertThat(resp.isAllowed()).isTrue())
                .verifyComplete();
    }

    // ---- multiple users ------------------------------------------------

    @Test
    @DisplayName("should give each key its own bucket")
    void shouldIsolateKeys() {
        limiter.addRouteConfig("iso", 1, 3);
        String keyA = "user:a";
        String keyB = "user:b";

        // Exhaust key A
        for (int i = 0; i < 3; i++) {
            limiter.isAllowed("iso", keyA).block();
        }
        assertThat(limiter.isAllowed("iso", keyA).block().isAllowed()).isFalse();

        // Key B should still have a full bucket
        assertThat(limiter.isAllowed("iso", keyB).block().isAllowed()).isTrue();
    }

    @Test
    @DisplayName("should isolate buckets by route as well as key")
    void shouldIsolateBucketsByRouteAndKey() {
        limiter.addRouteConfig("strict", 1, 2);
        limiter.addRouteConfig("relaxed", 10, 5);
        String key = "user:shared";

        assertThat(limiter.isAllowed("strict", key).block().isAllowed()).isTrue();
        assertThat(limiter.isAllowed("strict", key).block().isAllowed()).isTrue();
        assertThat(limiter.isAllowed("strict", key).block().isAllowed()).isFalse();

        assertThat(limiter.isAllowed("relaxed", key).block().isAllowed())
                .as("same user on another route should get an independent bucket")
                .isTrue();
    }

    // ---- contract methods ----------------------------------------------

    @Test
    @DisplayName("should expose correct config class")
    void shouldExposeCorrectConfigClass() {
        assertThat(limiter.getConfigClass()).isEqualTo(InMemoryRateLimiter.Config.class);
    }

    @Test
    @DisplayName("should return new config with default values")
    void shouldReturnNewDefaultConfig() {
        InMemoryRateLimiter.Config config = limiter.newConfig();
        assertThat(config.getReplenishRate())
                .isEqualTo(InMemoryRateLimiter.DEFAULT_CONFIG.getReplenishRate());
        assertThat(config.getBurstCapacity())
                .isEqualTo(InMemoryRateLimiter.DEFAULT_CONFIG.getBurstCapacity());
    }

    @Test
    @DisplayName("should expose route configs map")
    void shouldExposeRouteConfigs() {
        Map<String, InMemoryRateLimiter.Config> configs = limiter.getConfig();
        assertThat(configs).isNotNull();
        assertThat(configs).containsKey("default");
    }

    // ---- reset ---------------------------------------------------------

    @Test
    @DisplayName("should clear all buckets on reset")
    void shouldClearBucketsOnReset() {
        String key = "user:rst";
        limiter.addRouteConfig("rst", 1, 2);

        // Exhaust
        limiter.isAllowed("rst", key).block();
        limiter.isAllowed("rst", key).block();
        assertThat(limiter.isAllowed("rst", key).block().isAllowed()).isFalse();

        limiter.reset();
        assertThat(limiter.bucketCount()).isZero();

        // Fresh bucket after reset
        assertThat(limiter.isAllowed("rst", key).block().isAllowed()).isTrue();
    }

    @Test
    @DisplayName("should track bucket count correctly")
    void shouldTrackBucketCount() {
        assertThat(limiter.bucketCount()).isZero();
        limiter.isAllowed("test-default", "user:a").block();
        assertThat(limiter.bucketCount()).isEqualTo(1);
        limiter.isAllowed("test-default", "user:b").block();
        assertThat(limiter.bucketCount()).isEqualTo(2);
        // Same key twice should not increase count
        limiter.isAllowed("test-default", "user:a").block();
        assertThat(limiter.bucketCount()).isEqualTo(2);
    }

    // ---- config type ---------------------------------------------------

    @Nested
    @DisplayName("Config")
    class ConfigTests {

        @Test
        @DisplayName("should create config with specified values")
        void shouldCreateWithSpecifiedValues() {
            InMemoryRateLimiter.Config config = new InMemoryRateLimiter.Config(50, 100);
            assertThat(config.getReplenishRate()).isEqualTo(50);
            assertThat(config.getBurstCapacity()).isEqualTo(100);
        }

        @Test
        @DisplayName("should support setters")
        void shouldSupportSetters() {
            InMemoryRateLimiter.Config config = new InMemoryRateLimiter.Config();
            config.setReplenishRate(30);
            config.setBurstCapacity(60);
            assertThat(config.getReplenishRate()).isEqualTo(30);
            assertThat(config.getBurstCapacity()).isEqualTo(60);
        }
    }
}
