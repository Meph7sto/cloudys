package com.cloudys.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

/**
 * In-memory rate limiter using Bucket4j token-bucket algorithm.
 * <p>
 * Replaces the stub Resilience4j rate-limiter comment in pom.xml with a
 * working implementation.  Buckets are keyed by the resolved
 * {@code KeyResolver} id (user identity or client IP) so that each caller
 * gets its own budget.
 * <p>
 * Default limits: 100 requests / second with a burst of 200 (suitable for
 * most API endpoints).  Route-specific limits can be registered via
 * {@link #addRouteConfig(String, int, int)}.
 */
public class InMemoryRateLimiter implements RateLimiter<InMemoryRateLimiter.Config> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    /** Default configuration that applies when no route-specific config is registered. */
    static final Config DEFAULT_CONFIG = new Config(100, 200);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Config> routeConfigs = new ConcurrentHashMap<>();

    // ---- constructors --------------------------------------------------

    public InMemoryRateLimiter() {
        routeConfigs.put("default", DEFAULT_CONFIG);
    }

    // ---- RateLimiter contract ------------------------------------------

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        Config config = routeConfigs.getOrDefault(routeId, routeConfigs.get("default"));

        String bucketKey = routeId + "|" + id;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> createBucket(config));

        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            if (log.isTraceEnabled()) {
                log.trace("Rate-limit OK route={} key={} remaining={}", routeId, id, remaining);
            }
            Map<String, String> headers = new HashMap<>();
            headers.put("X-RateLimit-Remaining", String.valueOf(remaining));
            return Mono.just(new Response(true, headers));
        }

        log.debug("Rate-limit EXCEEDED route={} key={}", routeId, id);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Remaining", "0");
        return Mono.just(new Response(false, headers));
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public Config newConfig() {
        return new Config(DEFAULT_CONFIG.replenishRate, DEFAULT_CONFIG.burstCapacity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Config> getConfig() {
        return (Map<String, Config>) this.routeConfigs;
    }

    // ---- public helpers ------------------------------------------------

    /**
     * Register (or replace) the rate-limit configuration for a specific route.
     *
     * @param routeId        the Spring Cloud Gateway route id
     * @param replenishRate  tokens granted per second
     * @param burstCapacity  maximum tokens the bucket can hold
     */
    public void addRouteConfig(String routeId, int replenishRate, int burstCapacity) {
        routeConfigs.put(routeId, new Config(replenishRate, burstCapacity));
    }

    /** Remove all cached buckets (useful for testing). */
    public void reset() {
        buckets.clear();
    }

    /** Return the number of currently tracked buckets. */
    public int bucketCount() {
        return buckets.size();
    }

    // ---- private -------------------------------------------------------

    private static Bucket createBucket(Config config) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(config.getBurstCapacity())
                        .refillGreedy(config.getReplenishRate(), Duration.ofSeconds(1))
                        .initialTokens(config.getBurstCapacity())
                        .build())
                .build();
    }

    // ---- nested types --------------------------------------------------

    /**
     * Per-route rate-limit parameters.
     */
    public static class Config {

        private int replenishRate;
        private int burstCapacity;

        public Config() {
            this(DEFAULT_CONFIG.replenishRate, DEFAULT_CONFIG.burstCapacity);
        }

        public Config(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }
}
