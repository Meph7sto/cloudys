package com.cloudys.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

/**
 * Gateway rate-limiting configuration.
 */
@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    InMemoryRateLimiter inMemoryRateLimiter(GatewayRateLimitProperties properties) {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        GatewayRateLimitProperties.RouteLimit defaultLimit = properties.getDefault();
        limiter.addRouteConfig("default", defaultLimit.getReplenishRate(), defaultLimit.getBurstCapacity());

        properties.getRoutes().forEach((routeId, routeLimit) ->
                limiter.addRouteConfig(routeId, routeLimit.getReplenishRate(), routeLimit.getBurstCapacity()));

        if (!properties.isEnabled()) {
            log.info("Gateway rate limiting properties loaded but feature flag is disabled");
        } else {
            log.info(
                    "InMemoryRateLimiter initialised — default {} req/s burst {}, {} route overrides",
                    defaultLimit.getReplenishRate(),
                    defaultLimit.getBurstCapacity(),
                    properties.getRoutes().size()
            );
        }

        return limiter;
    }
}
